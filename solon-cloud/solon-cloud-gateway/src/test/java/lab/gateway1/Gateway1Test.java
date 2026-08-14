/*
 * Copyright 2017-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package lab.gateway1;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.util.MimeType;
import org.noear.solon.net.http.HttpResponse;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonTest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author noear 2024/8/16 created
 */
@SolonTest(Gateway1Main.class)
public class Gateway1Test extends HttpTester {
    @Test
    public void WebTest() throws Exception {
        String rst = path("/hello?name=noear").get();
        assert rst != null;
        assert rst.equals("noear");
    }

    @Test
    public void GatewayGetSslTest() throws Exception {
        HttpResponse resp = path("/h5/more.htm").exec("GET");

        String rst = resp.bodyAsString();
        System.out.println(rst);
        assert rst != null;
        assert rst.contains("设置");
    }

    @Test
    public void GatewayGetTest() throws Exception {
        HttpResponse resp = path("/demo/test?name=noear").exec("GET");

        assert "1".equals(resp.header("Test-V"));

        String rst = resp.bodyAsString();
        assert rst != null;
        assert rst.equals("noear");
    }


    @Test
    public void GatewayPostTest() throws Exception {
        StringBuilder buf = new StringBuilder();
        while (buf.length() < 1024 * 1024) { //1m
            buf.append("noear0123456789abcdef");
        }


        String rst = path("/demo/test?p1=1").data("name", buf.toString()).post();
        assert rst != null;
        assert rst.contains("noear");
    }

    @Test
    public void GatewayPostBodyTest() throws Exception {
        StringBuilder buf = new StringBuilder();
        while (buf.length() < 1024 * 1024 * 1) { //1m
            buf.append("noear0123456789abcdef");
        }

        String rst = path("/demo/test").bodyOfJson("{\"name\":\"" + buf + "\"}").post();
        assert rst != null;
        assert rst.contains("noear");
    }

    @Test
    public void GatewayPostBodyReplace() throws Exception {
        String rst = path("/demo/body?r=1")
                .body("noear", MimeType.TEXT_PLAIN_VALUE)
                .post();

        assert rst != null;
        assert rst.contains("hello-测试");
    }

    @Test
    public void GatewayPostBigBodyTest() throws Exception {
        StringBuilder buf = new StringBuilder();
        while (buf.length() < 1024 * 1024 * 8) { //8m
            buf.append("noear0123456789abcdef");
        }

        int code = path("/demo/test").bodyOfJson("{\"name\":\"" + buf + "\"}").execAsCode("POST");
        assert code == 413;
    }

    @Test
    public void GatewayPostErrorTest() throws Exception {
        HttpResponse resp = path("/demo/error").bodyOfJson("{}").exec("POST");
        assert resp.code() == 413;
        assert "hello".equals(resp.bodyAsString());
    }

    @Test
    public void GatewayUploadFileTest() throws Exception {
        StringBuilder fileBuf = new StringBuilder();
        while (fileBuf.length() < 1024 * 1024 * 1) { //8m
            fileBuf.append("noear0123456789abcdef");
        }
        ByteArrayInputStream fileData = new ByteArrayInputStream(fileBuf.toString().getBytes());

        String rst = path("/demo/upload")
                .data("file", "test.md", fileData, "text/md")
                .data("attrIds","1")
                .data("attrIds","2")
                .post();
        assert rst != null;
        assert "test.md".equals(rst);
    }

    /**
     * WebSocket 文本消息转发：经网关 ws-test 路由（ws://localhost:8080）透传到上游 DemoWebSocket
     *
     * <p>连接带 t=1 参数（上游 onOpen 校验，否则会被关闭）；发文本 "hello"，期待回 "hello--pong"。</p>
     */
    @Test
    public void GatewayWsTest() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();

        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost("localhost")
                .setPort(8900)
                .setURI("/ws/test?t=1");

        CountDownLatch countDownLatch = new CountDownLatch(1);
        AtomicReference<String> messageRef = new AtomicReference<>();

        client.webSocket(options, result -> {
            if (result.succeeded()) {
                WebSocket ws = result.result();
                System.out.println("WebSocket 连接成功!");

                // 设置文本消息处理器
                ws.textMessageHandler(message -> {
                    System.out.println("收到消息: " + message);

                    messageRef.set(message);
                    assert message.equals("hello--pong");
                    countDownLatch.countDown();
                });

                // 发送文本消息
                ws.writeTextMessage("hello");

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
        assert "hello--pong".equals(messageRef.get());

        //关闭 vertx，防测试进程资源泄漏（close 为异步，调用即发起）
        vertx.close();
    }

    /**
     * WebSocket 二进制消息转发：同样经网关透传，发二进制 "hello"，期待回 "hello--pong"
     */
    @Test
    public void GatewayWsBinaryTest() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpClient client = vertx.createHttpClient();

        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setHost("localhost")
                .setPort(8900)
                .setURI("/ws/bin?t=1");

        CountDownLatch countDownLatch = new CountDownLatch(1);

        client.webSocket(options, result -> {
            if (result.succeeded()) {
                WebSocket ws = result.result();
                System.out.println("WebSocket 连接成功!");

                // 设置二进制消息处理器
                ws.binaryMessageHandler(buffer -> {
                    String message = buffer.toString(StandardCharsets.UTF_8);
                    System.out.println("收到消息: " + message);

                    assert message.equals("hello--pong");
                    countDownLatch.countDown();
                });

                // 发送二进制消息
                ws.writeBinaryMessage(Buffer.buffer("hello"));

                vertx.setTimer(5000, id -> {
                    ws.close();
                });
            } else {
                System.err.println("WebSocket 连接失败: " + result.cause().getMessage());
                countDownLatch.countDown();
            }
        });

        assert countDownLatch.await(10, TimeUnit.SECONDS);
        assert countDownLatch.getCount() == 0;

        vertx.close();
    }
}
