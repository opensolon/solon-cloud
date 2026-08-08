package features.gateway;

import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExPredicate;
import org.noear.solon.cloud.gateway.properties.GatewayProperties;
import org.noear.solon.cloud.gateway.properties.HttpClientProperties;
import org.noear.solon.cloud.gateway.properties.HttpProxyProperties;
import org.noear.solon.cloud.gateway.properties.HttpSslProperties;
import org.noear.solon.cloud.gateway.properties.XForwardedProperties;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.cloud.gateway.route.predicate.HeaderPredicateFactory;
import org.noear.solon.cloud.gateway.route.predicate.QueryPredicateFactory;
import org.noear.solon.cloud.gateway.route.predicate.RemoteAddrPredicateFactory;
import org.noear.solon.cloud.gateway.route.predicate.XForwardedRemoteAddrPredicateFactory;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 网关优化验证（纯逻辑单测，不启动服务器）
 *
 * 覆盖：P0-3 XForwardedRemoteAddr 去 DNS 化、P1-4 正则长度防护与输入预检、
 * RemoteAddr 纯对端匹配、x-forwarded 出站头默认值、
 * P2 httpclient.proxy/ssl/compression 默认值与谓词/过滤器工厂 enabled 开关
 *
 * @author noear
 * @since 4.0.4
 */
public class GatewayOptimizationTest {
    //对端 socket 地址 mock（供 RemoteAddr 纯对端谓词测试）
    private SocketAddress remoteAddr;

    private ExContext ctxOf(String realIp, String headerName, String headerValue, String queryName, String queryValue) {
        return (ExContext) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ExContext.class},
                (proxy, method, args) -> {
                    String name = method.getName();

                    if ("realIp".equals(name)) {
                        return realIp;
                    }

                    if ("remoteAddress".equals(name)) {
                        return remoteAddr;
                    }

                    if ("rawHeader".equals(name) && args != null && args.length == 1 && headerName.equals(args[0])) {
                        return headerValue;
                    }

                    if ("rawQueryParam".equals(name) && args != null && args.length == 1 && queryName.equals(args[0])) {
                        return queryValue;
                    }

                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    }
                    if (rt == int.class) {
                        return 0;
                    }
                    if (rt == long.class) {
                        return 0L;
                    }
                    return null;
                });
    }

    /**
     * P0-3：XForwardedRemoteAddr 谓词拒绝主机名（此前会触发事件循环上的同步 DNS），IP 字面量正常匹配
     */
    @Test
    public void xForwardedRemoteAddrPredicate_rejectHostname_withoutDns() {
        ExPredicate predicate = new XForwardedRemoteAddrPredicateFactory().create("192.168.1.0/24");

        //主机名（慢 DNS 攻击向量）：必须不匹配，且不抛异常
        assertFalse(predicate.test(ctxOf("evil-host.attacker.io", null, null, null, null)));
        assertFalse(predicate.test(ctxOf("localhost", null, null, null, null)));

        //IPv4 字面量正常匹配
        assertTrue(predicate.test(ctxOf("192.168.1.100", null, null, null, null)));
        assertFalse(predicate.test(ctxOf("10.0.0.1", null, null, null, null)));

        //IPv6 字面量不抛异常
        assertFalse(predicate.test(ctxOf("fe80::1", null, null, null, null)));

        //IPv6 压缩写法（冒号开头，如 ::1）应走字面量判定（此前会被误拒）
        ExPredicate v6Rule = new XForwardedRemoteAddrPredicateFactory().create("::1/128");
        assertTrue(v6Rule.test(ctxOf("::1", null, null, null, null)));
        assertFalse(v6Rule.test(ctxOf("fe80::1", null, null, null, null)));

        //空 IP 不匹配
        assertFalse(predicate.test(ctxOf("", null, null, null, null)));
    }

    /**
     * RemoteAddr（socket 版）：匹配纯 TCP 对端地址，不信任客户端头（防伪造头绕过 ACL）
     */
    @Test
    public void remoteAddrPredicate_matchSocketPeer_ignoreHeaders() {
        ExPredicate predicate = new RemoteAddrPredicateFactory().create("192.168.1.0/24");

        //对端在网段内：即使 X-Real-IP 伪造为网段外地址，也按对端地址判定（不信任头）
        remoteAddr = SocketAddress.inetSocketAddress(12345, "192.168.1.100");
        assertTrue(predicate.test(ctxOf("10.0.0.1", null, null, null, null)));

        //对端不在网段内：即使 X-Real-IP 伪造为网段内地址，也不匹配
        remoteAddr = SocketAddress.inetSocketAddress(12345, "10.0.0.1");
        assertFalse(predicate.test(ctxOf("192.168.1.100", null, null, null, null)));

        //对端地址为 null（防御）：不匹配，不抛异常
        remoteAddr = null;
        assertFalse(predicate.test(ctxOf("192.168.1.100", null, null, null, null)));

        //主机名对端：非字面量直接不匹配（避免同步 DNS），不抛异常
        remoteAddr = SocketAddress.inetSocketAddress(12345, "fe80::1");
        assertFalse(predicate.test(ctxOf("192.168.1.100", null, null, null, null)));
    }

    /**
     * x-forwarded 出站头配置默认值：总开关开启，XFF 追加、Host/Port/Proto 覆盖（兼容升级前 X-Forwarded-Host 行为）
     */
    @Test
    public void xForwardedProperties_defaults_aligned() {
        XForwardedProperties props = new XForwardedProperties();

        //总开关默认开启
        assertTrue(props.isEnabled());

        //X-Forwarded-For：默认逐跳追加
        assertTrue(props.isForEnabled());
        assertTrue(props.isForAppend());

        //X-Forwarded-Host：默认覆盖式
        assertTrue(props.isHostEnabled());
        assertFalse(props.isHostAppend());

        //X-Forwarded-Port / X-Forwarded-Proto：默认覆盖式
        assertTrue(props.isPortEnabled());
        assertFalse(props.isPortAppend());
        assertTrue(props.isProtoEnabled());
        assertFalse(props.isProtoAppend());
    }

    /**
     * P1-4：Header 谓词正则长度防护（配置防呆）
     */
    @Test
    public void headerPredicate_regexLengthGuard() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append('a');
        }

        //超长正则：启动期 fail-fast
        assertThrows(IllegalArgumentException.class,
                () -> new HeaderPredicateFactory().create("X-Token," + sb));
    }

    /**
     * P1-4：Header 谓词输入长度预检（防灾难性回溯放大）+ 正常匹配
     */
    @Test
    public void headerPredicate_inputLengthPrecheck_andNormalMatch() {
        ExPredicate predicate = new HeaderPredicateFactory().create("X-Token,^user");

        //正常匹配
        assertTrue(predicate.test(ctxOf(null, "X-Token", "user-1", null, null)));
        assertFalse(predicate.test(ctxOf(null, "X-Token", "admin", null, null)));

        //超长输入（>4096）：直接不匹配，避免正则灾难性回溯
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            big.append('a');
        }
        assertFalse(predicate.test(ctxOf(null, "X-Token", big.toString(), null, null)));

        //头不存在：不匹配
        assertFalse(predicate.test(ctxOf(null, "X-Other", "user-1", null, null)));

        //无正则：存在即匹配
        ExPredicate existsOnly = new HeaderPredicateFactory().create("X-Token");
        assertTrue(existsOnly.test(ctxOf(null, "X-Token", "anything", null, null)));
    }

    /**
     * P1-4：Query 谓词同样具备正则长度防护与输入预检
     */
    @Test
    public void queryPredicate_regexLengthGuard_andPrecheck() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            sb.append('a');
        }
        assertThrows(IllegalArgumentException.class,
                () -> new QueryPredicateFactory().create("token," + sb));

        ExPredicate predicate = new QueryPredicateFactory().create("token,^user");
        assertTrue(predicate.test(ctxOf(null, null, null, "token", "user-abc")));
        assertFalse(predicate.test(ctxOf(null, null, null, "token", "other")));

        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            big.append('a');
        }
        assertFalse(predicate.test(ctxOf(null, null, null, "token", big.toString())));
    }

    /**
     * P2：httpClient 新增 proxy / ssl / compression 默认值
     */
    @Test
    public void httpClientProperties_proxySslCompression_defaults() {
        HttpClientProperties props = new HttpClientProperties();

        //compression：默认关闭
        assertFalse(props.isCompression());
        props.setCompression(true);
        assertTrue(props.isCompression());

        //proxy：默认关闭、端口 8080、类型 HTTP
        HttpProxyProperties proxy = props.getProxy();
        assertFalse(proxy.isEnabled());
        assertEquals(8080, proxy.getPort());
        assertEquals("HTTP", proxy.getType());

        proxy.setEnabled(true);
        proxy.setHost("127.0.0.1");
        proxy.setPort(3128);
        proxy.setType("SOCKS5");
        proxy.setUsername("u");
        proxy.setPassword("p");
        proxy.setNonProxyHostsPattern("localhost|127.*");
        assertTrue(proxy.isEnabled());
        assertEquals("127.0.0.1", proxy.getHost());
        assertEquals(3128, proxy.getPort());
        assertEquals("SOCKS5", proxy.getType());
        assertEquals("u", proxy.getUsername());
        assertEquals("p", proxy.getPassword());
        assertEquals("localhost|127.*", proxy.getNonProxyHostsPattern());

        //ssl：默认关闭，配置后生效
        HttpSslProperties ssl = props.getSsl();
        assertFalse(ssl.isEnabled());
        ssl.setEnabled(true);
        ssl.setKeyStore("/etc/gateway/client.p12");
        ssl.setKeyStorePassword("secret");
        ssl.setKeyStoreType("PKCS12");
        ssl.setKeyPassword("keypass");
        ssl.setTrustStore("/etc/gateway/trust.jks");
        ssl.setTrustStorePassword("trustsecret");
        assertTrue(ssl.isEnabled());
        assertEquals("/etc/gateway/client.p12", ssl.getKeyStore());
        assertEquals("PKCS12", ssl.getKeyStoreType());
        assertEquals("keypass", ssl.getKeyPassword());
        assertEquals("/etc/gateway/trust.jks", ssl.getTrustStore());
    }

    /**
     * P2：谓词/过滤器工厂启用开关（solon.cloud.gateway.predicate.*.enabled / filter.*.enabled
     */
    @Test
    public void predicateFilterFactory_enabledSwitch() {
        GatewayProperties gatewayProperties = new GatewayProperties();
        //关闭 RemoteAddr 谓词与 StripPrefix 过滤器（配置键为小写连字符，工厂 prefix 为驼峰，规范化后匹配）
        gatewayProperties.getPredicate().put("remote-addr", false);
        gatewayProperties.getFilter().put("strip-prefix", false);

        Vertx vertx = Vertx.vertx();
        try {
            RouteFactoryManager manager = new RouteFactoryManager(vertx, gatewayProperties);

            //被关闭的工厂：构建返回 null（视为未注册，不报错）
            assertNull(manager.getPredicate("RemoteAddr", "10.0.0.0/8"));
            assertNull(manager.getFilter("StripPrefix", "1"));

            //未关闭的工厂：正常构建
            assertNotNull(manager.getPredicate("XForwardedRemoteAddr", "10.0.0.0/8"));
            assertNotNull(manager.getFilter("AddRequestHeader", "X-Debug,1"));

            //缺省（无开关配置）：全部可用
            RouteFactoryManager defaultManager = new RouteFactoryManager(vertx, new GatewayProperties());
            assertNotNull(defaultManager.getPredicate("RemoteAddr", "10.0.0.0/8"));
            assertNotNull(defaultManager.getFilter("StripPrefix", "1"));
        } finally {
            vertx.close();
        }
    }
}
