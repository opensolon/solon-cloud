package features.gateway.sys;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketConnectOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.cloud.impl.CloudLoadBalanceFactory;
import org.noear.solon.core.LoadBalance;
import org.noear.solon.core.util.MimeType;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author noear 2024/10/1 created
 */
@SolonTest(args = "--cfg=sys.yml", enableHttp = true, enableWebSocket = true)
public class GatewayTest extends HttpTester {
    @Test
    public void hello() throws Exception {
        assert "hello".equals(path("/hello").get());
    }

    @Test
    public void file() throws Exception {
        assert "hello.txt".equals(path("/file").data(
                "file",
                "hello.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
                MimeType.TEXT_PLAIN_VALUE).post());
    }

    @Test
    public void gateway_hello() throws Exception {
        assert "hello".equals(path("/test/hello").get());
    }

    @Test
    public void gateway_file() throws Exception {
        assert "hello.txt".equals(path("/test/file").data(
                "file",
                "hello.txt",
                new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
                MimeType.TEXT_PLAIN_VALUE).post());
    }

    @Test
    public void gateway_hello_json() throws Exception {
        assert "hello".equals(path("/test/hello").bodyOfJson("{test:1}").post());
    }

    @Test
    public void gateway_hello_form() throws Exception {
        assert "hello".equals(path("/test/hello").data("test","1").post());
    }

    /**
     * 4.0.5 回归：网关转发文件上传到"不支持 chunked 请求体"的后端 http-server
     *
     * <p>极简后端：收到 Transfer-Encoding: chunked 请求体即回 411（模拟部分老式 http-server），
     * 否则按固定长度读取 body 回 200 ok:len。修复前网关无条件剥离 Content-Length，
     * 流式转发被 Vert.x 自动改为 chunked → 后端 411；修复后未修改的流式请求保留原始
     * Content-Length 固定长度转发 → 200 ok。</p>
     */
    @Test
    public void gateway_upload_fixedLengthBackend() throws Exception {
        Vertx vertx = Vertx.vertx();
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<Throwable> bindErr = new AtomicReference<>();

        HttpServer backend = vertx.createHttpServer();
        backend.requestHandler(req -> {
            if (req.headers().contains("Transfer-Encoding")) {
                //不支持 chunked 请求体：411
                req.response().setStatusCode(411).end("chunked not supported");
            } else {
                req.body().onSuccess(buf -> {
                    req.response().end("ok:" + buf.length());
                }).onFailure(err -> {
                    req.response().setStatusCode(500).end("body read fail");
                });
            }
        });
        backend.listen(18091, "127.0.0.1", ar -> {
            if (ar.succeeded()) {
                ready.countDown();
            } else {
                bindErr.set(ar.cause());
                ready.countDown();
            }
        });

        try {
            assertTrue(ready.await(10, TimeUnit.SECONDS), "backend start timeout");
            assertNull(bindErr.get(), "backend bind fail: " + bindErr.get());

            String rst = path("/fl/upload")
                    .data("file",
                            "hello.txt",
                            new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)),
                            MimeType.TEXT_PLAIN_VALUE)
                    .post();

            assertNotNull(rst, "no response body");
            assertTrue(rst.startsWith("ok:"), "expected fixed-length forward, got: " + rst);
        } finally {
            backend.close();
            vertx.close();
        }
    }

    @Test
    public void gateway_ws() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();

        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost("localhost")
                .setPort(8079)
                .setURI("/test_ws/ws");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        client.webSocket(options, result -> {
            if (result.succeeded()) {
                WebSocket ws = result.result();
                System.out.println("WebSocket 连接成功!");

                // 设置消息处理器
                ws.textMessageHandler(message -> {
                    System.out.println("收到消息: " + message);

                    countDownLatch.countDown();;
                });

                // 发送消息
                ws.writeTextMessage("Hello, WebSocket Server!");

                // 延迟后关闭连接
                vertx.setTimer(5000, id -> {
                    ws.close();
                });
            } else {
                System.err.println("WebSocket 连接失败: " + result.cause().getMessage());
                countDownLatch.countDown();
            }
        });

        //带超时等待，防止测试失败时无限挂起
        assert countDownLatch.await(10, TimeUnit.SECONDS);
        assert countDownLatch.getCount() == 0;

        //关闭 vertx，防测试进程资源泄漏（close 为异步，调用即发起）
        vertx.close();
    }

    //----------------

    @Test
    public void gateway_h5() throws Exception {
        assert path("/www/h5/").get().contains("H5浏览器");
    }

    @Test
    public void gateway_solon() throws Exception {
        assert path("/www/").get().contains("Solon官网");
    }

    @Test
    public void gateway_solon2() throws Exception {
        assert path("/ZZZ/").get().contains("Solon官网");
    }
}
