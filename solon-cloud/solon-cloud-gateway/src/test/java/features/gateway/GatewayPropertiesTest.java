package features.gateway;

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.properties.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * properties 配置类全覆盖（构造器 / getter / setter 全分支）
 *
 * 覆盖：TimeoutProperties、RouteProperties、DiscoverProperties、HttpPoolProperties、
 * GatewayProperties、HttpClientProperties、HttpProxyProperties、HttpSslProperties、
 * WebSocketProperties、XForwardedProperties
 *
 * @author noear
 * @since 4.0.4
 */
public class GatewayPropertiesTest {

    /**
     * TimeoutProperties：三构造器 + 全 getter/setter
     */
    @Test
    public void timeoutProperties_allConstructors_andSetters() {
        TimeoutProperties p1 = new TimeoutProperties();
        assertEquals(10, p1.getConnectTimeout());
        assertEquals(10, p1.getRequestTimeout());
        assertEquals(60, p1.getResponseTimeout());

        //单参构造：三值相同
        TimeoutProperties p2 = new TimeoutProperties(30);
        assertEquals(30, p2.getConnectTimeout());
        assertEquals(30, p2.getRequestTimeout());
        assertEquals(30, p2.getResponseTimeout());

        //三参构造 + setter 覆盖
        TimeoutProperties p3 = new TimeoutProperties(1, 2, 3);
        p3.setConnectTimeout(11);
        p3.setRequestTimeout(22);
        p3.setResponseTimeout(33);
        assertEquals(11, p3.getConnectTimeout());
        assertEquals(22, p3.getRequestTimeout());
        assertEquals(33, p3.getResponseTimeout());
    }

    /**
     * RouteProperties：只读属性类，getter 全分支（null/0 默认）
     */
    @Test
    public void routeProperties_getters() {
        RouteProperties p = new RouteProperties();
        assertNull(p.getId());
        assertEquals(0, p.getIndex());
        assertNull(p.getTarget());
        assertNull(p.getPredicates());
        assertNull(p.getFilters());
        assertNull(p.getTimeout());
    }

    /**
     * DiscoverProperties：enabled + 服务列表默认值
     */
    @Test
    public void discoverProperties_getters() {
        DiscoverProperties p = new DiscoverProperties();
        assertFalse(p.isEnabled());
        assertNotNull(p.getExcludedServices());
        assertTrue(p.getExcludedServices().isEmpty());
        assertNotNull(p.getIncludedServices());
        assertTrue(p.getIncludedServices().isEmpty());
    }

    /**
     * HttpPoolProperties：默认值 + 全 setter
     */
    @Test
    public void httpPoolProperties_defaults_andSetters() {
        HttpPoolProperties p = new HttpPoolProperties();
        assertEquals(250, p.getMaxConnections());
        assertEquals(1000, p.getMaxWaitQueueSize());
        assertEquals(60, p.getMaxIdleTime());
        assertEquals(60, p.getKeepAliveTimeout());

        p.setMaxConnections(100);
        p.setMaxWaitQueueSize(-1);
        p.setMaxIdleTime(30);
        p.setKeepAliveTimeout(90);
        assertEquals(100, p.getMaxConnections());
        assertEquals(-1, p.getMaxWaitQueueSize());
        assertEquals(30, p.getMaxIdleTime());
        assertEquals(90, p.getKeepAliveTimeout());
    }

    /**
     * GatewayProperties：懒初始化 getter（先 get 触发）+ 子属性默认 + 开关 Map
     */
    @Test
    public void gatewayProperties_lazyInit_andSubProperties() {
        GatewayProperties p = new GatewayProperties();

        //懒初始化：首次 get 触发（非 null）
        assertNotNull(p.getRoutes());
        assertTrue(p.getRoutes().isEmpty());
        //defaultFilters 不懒初始化（null）
        assertNull(p.getDefaultFilters());

        //子属性默认值
        assertNotNull(p.getHttpClient());
        assertNotNull(p.getHttpClient().getPool());
        assertNotNull(p.getHttpClient().getWebsocket());
        assertNotNull(p.getHttpClient().getProxy());
        assertNotNull(p.getHttpClient().getSsl());
        assertNotNull(p.getXForwarded());
        assertNotNull(p.getDiscover());

        //谓词/过滤器开关 Map：懒初始化 + put/get
        assertNotNull(p.getPredicate());
        assertTrue(p.getPredicate().isEmpty());
        p.getPredicate().put("remote-addr", false);
        assertEquals(false, p.getPredicate().get("remote-addr"));
        p.setPredicate(new java.util.HashMap<>());

        assertNotNull(p.getFilter());
        assertTrue(p.getFilter().isEmpty());
        p.getFilter().put("strip-prefix", false);
        assertEquals(false, p.getFilter().get("strip-prefix"));
        p.setFilter(new java.util.HashMap<>());
    }

    /**
     * HttpClientProperties：默认值 + compression/proxy/ssl 开关 + pool/websocket 子组
     */
    @Test
    public void httpClientProperties_defaults_andChildren() {
        HttpClientProperties p = new HttpClientProperties();
        //继承 TimeoutProperties 默认
        assertEquals(10, p.getConnectTimeout());
        assertEquals(10, p.getRequestTimeout());
        assertEquals(60, p.getResponseTimeout());

        //compression 默认关
        assertFalse(p.isCompression());
        p.setCompression(true);
        assertTrue(p.isCompression());

        //pool 子组 + setter
        assertEquals(250, p.getPool().getMaxConnections());
        p.setPool(new HttpPoolProperties());
        //websocket 子组 + setter
        assertNotNull(p.getWebsocket());
        p.setWebsocket(new WebSocketProperties());
        //proxy/ssl 子组默认关 + setter
        assertFalse(p.getProxy().isEnabled());
        p.setProxy(new HttpProxyProperties());
        assertFalse(p.getSsl().isEnabled());
        p.setSsl(new HttpSslProperties());
    }

    /**
     * HttpProxyProperties：默认值 + 全 setter
     */
    @Test
    public void httpProxyProperties_defaults_andSetters() {
        HttpProxyProperties p = new HttpProxyProperties();
        assertFalse(p.isEnabled());
        assertNull(p.getHost());
        assertEquals(8080, p.getPort());
        assertEquals("HTTP", p.getType());
        assertNull(p.getUsername());
        assertNull(p.getPassword());
        assertNull(p.getNonProxyHostsPattern());

        p.setEnabled(true);
        p.setHost("10.0.0.1");
        p.setPort(3128);
        p.setType("SOCKS5");
        p.setUsername("user");
        p.setPassword("pass");
        p.setNonProxyHostsPattern("localhost|127.*");
        assertTrue(p.isEnabled());
        assertEquals("10.0.0.1", p.getHost());
        assertEquals(3128, p.getPort());
        assertEquals("SOCKS5", p.getType());
        assertEquals("user", p.getUsername());
        assertEquals("pass", p.getPassword());
        assertEquals("localhost|127.*", p.getNonProxyHostsPattern());
    }

    /**
     * HttpSslProperties：默认值 + 全 setter（keyPassword 缺省回退）
     */
    @Test
    public void httpSslProperties_defaults_andSetters() {
        HttpSslProperties p = new HttpSslProperties();
        assertFalse(p.isEnabled());
        assertNull(p.getKeyStore());
        assertNull(p.getKeyStorePassword());
        //证书库类型缺省 null（ClientOptionsUtil 按 null→JKS 兜底）
        assertNull(p.getKeyStoreType());
        assertNull(p.getKeyPassword());
        assertNull(p.getTrustStore());
        assertNull(p.getTrustStorePassword());
        assertNull(p.getTrustStoreType());

        p.setEnabled(true);
        p.setKeyStore("/etc/gateway/client.p12");
        p.setKeyStorePassword("secret");
        p.setKeyStoreType("PKCS12");
        p.setKeyPassword("keypass");
        p.setTrustStore("/etc/gateway/trust.jks");
        p.setTrustStorePassword("trustsecret");
        p.setTrustStoreType("PKCS12");
        assertTrue(p.isEnabled());
        assertEquals("/etc/gateway/client.p12", p.getKeyStore());
        assertEquals("secret", p.getKeyStorePassword());
        assertEquals("PKCS12", p.getKeyStoreType());
        assertEquals("keypass", p.getKeyPassword());
        assertEquals("/etc/gateway/trust.jks", p.getTrustStore());
        assertEquals("trustsecret", p.getTrustStorePassword());
        assertEquals("PKCS12", p.getTrustStoreType());
    }

    /**
     * WebSocketProperties：默认值 + 全 setter
     */
    @Test
    public void webSocketProperties_defaults_andSetters() {
        WebSocketProperties p = new WebSocketProperties();
        assertEquals(200, p.getMaxConnections());
        assertEquals(60, p.getIdleTimeout());
        assertEquals(10, p.getClosingTimeout());
        assertEquals(30, p.getPingInterval());

        p.setMaxConnections(500);
        p.setIdleTimeout(120);
        p.setClosingTimeout(20);
        p.setPingInterval(10);
        assertEquals(500, p.getMaxConnections());
        assertEquals(120, p.getIdleTimeout());
        assertEquals(20, p.getClosingTimeout());
        assertEquals(10, p.getPingInterval());
    }

    /**
     * XForwardedProperties：默认值（for 追加其余覆盖）+ 全 setter
     */
    @Test
    public void xForwardedProperties_defaults_andSetters() {
        XForwardedProperties p = new XForwardedProperties();
        assertTrue(p.isEnabled());
        assertTrue(p.isForEnabled());
        assertTrue(p.isForAppend());
        assertTrue(p.isHostEnabled());
        assertFalse(p.isHostAppend());
        assertTrue(p.isPortEnabled());
        assertFalse(p.isPortAppend());
        assertTrue(p.isProtoEnabled());
        assertFalse(p.isProtoAppend());

        p.setEnabled(false);
        p.setForEnabled(false);
        p.setForAppend(false);
        p.setHostEnabled(false);
        p.setHostAppend(true);
        p.setPortEnabled(false);
        p.setPortAppend(true);
        p.setProtoEnabled(false);
        p.setProtoAppend(true);
        assertFalse(p.isEnabled());
        assertFalse(p.isForEnabled());
        assertFalse(p.isForAppend());
        assertFalse(p.isHostEnabled());
        assertTrue(p.isHostAppend());
        assertFalse(p.isPortEnabled());
        assertTrue(p.isPortAppend());
        assertFalse(p.isProtoEnabled());
        assertTrue(p.isProtoAppend());
    }
}
