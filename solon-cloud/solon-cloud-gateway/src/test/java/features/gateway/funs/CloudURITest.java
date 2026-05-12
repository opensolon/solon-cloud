package features.gateway.funs;

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.utils.CloudURI;

import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author noear 2026/5/12 created
 *
 */
public class CloudURITest {
    @Test
    public void case1() throws URISyntaxException {
        // 标准URI
        CloudURI u1 = new CloudURI("http://example.com/api");
        System.out.println("协议数组: " + String.join(",", u1.getSchemes()));  // http
        System.out.println("内层URI: " + u1.getTargetUri());                      // example.com/api
        System.out.println("重建: " + u1);                                       // http://example.com/api

        // 验证协议数组
        String[] protocols = u1.getSchemes();
        assertNotNull(protocols);
        assertEquals(1, protocols.length);
        assertEquals("http", protocols[0]);

        // 验证内层URI
        assertNotNull(u1.getTargetUri());
        assertEquals("http", u1.getTargetUri().getScheme());
        assertEquals("example.com", u1.getTargetUri().getHost());
        assertEquals(-1, u1.getTargetUri().getPort());
        assertEquals("/api", u1.getTargetUri().getPath());

        // 验证重建
        assertEquals("http://example.com/api", u1.toString());
    }

    @Test
    public void case2() throws URISyntaxException {
        // 两级协议
        CloudURI u2 = new CloudURI("lb:ws://a.b.c:8080/chat");
        System.out.println("\n协议数组: " + String.join(",", u2.getSchemes())); // lb,ws
        System.out.println("内层URI协议: " + u2.getTargetUri().getScheme());      // ws
        System.out.println("内层URI主机: " + u2.getTargetUri().getHost());        // a.b.c
        System.out.println("内层URI端口: " + u2.getTargetUri().getPort());        // 8080
        System.out.println("内层URI路径: " + u2.getTargetUri().getPath());        // /chat
        System.out.println("重建: " + u2);                                       // lb:ws://a.b.c:8080/chat

        // 验证协议数组
        String[] protocols = u2.getSchemes();
        assertNotNull(protocols);
        assertEquals(2, protocols.length);
        assertEquals("lb", protocols[0]);
        assertEquals("ws", protocols[1]);

        // 验证内层URI
        assertNotNull(u2.getTargetUri());
        assertEquals("ws", u2.getTargetUri().getScheme());
        assertEquals("a.b.c", u2.getTargetUri().getHost());
        assertEquals(8080, u2.getTargetUri().getPort());
        assertEquals("/chat", u2.getTargetUri().getPath());

        // 验证重建
        assertEquals("lb:ws://a.b.c:8080/chat", u2.toString());
    }

    @Test
    public void case3() throws URISyntaxException {
        // 三级协议
        CloudURI u3 = new CloudURI("custom:lb:ws://x.y.z/path");
        System.out.println("\n协议数组: " + String.join(",", u3.getSchemes())); // custom,lb,ws
        System.out.println("内层URI: " + u3.getTargetUri());                      // x.y.z/path
        System.out.println("重建: " + u3);                                       // custom:lb:ws://x.y.z/path

        // 验证协议数组
        String[] protocols = u3.getSchemes();
        assertNotNull(protocols);
        assertEquals(3, protocols.length);
        assertEquals("custom", protocols[0]);
        assertEquals("lb", protocols[1]);
        assertEquals("ws", protocols[2]);

        // 验证内层URI
        assertNotNull(u3.getTargetUri());
        assertEquals("ws", u3.getTargetUri().getScheme());
        assertEquals("x.y.z", u3.getTargetUri().getHost());
        assertEquals(-1, u3.getTargetUri().getPort());
        assertEquals("/path", u3.getTargetUri().getPath());

        // 验证重建
        assertEquals("custom:lb:ws://x.y.z/path", u3.toString());
    }

    @Test
    public void case4_error() {
        // 测试无效URI（缺少://）
        assertThrows(URISyntaxException.class, () -> {
            new CloudURI("invalid-uri");
        });
    }

    @Test
    public void case5_empty_protocol() throws URISyntaxException {
        // 测试空协议段（应该能正常处理）
        CloudURI u = new CloudURI("http://example.com");
        String[] protocols = u.getSchemes();
        assertEquals(1, protocols.length);
        assertEquals("http", protocols[0]);
    }
}