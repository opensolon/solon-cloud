package features.gateway.sys;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
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

    @Test
    public void gateway_ws() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client =vertx.createHttpClient();

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
                vertx.close();
            }
        });

        countDownLatch.await();

        assert countDownLatch.getCount() == 0;
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
