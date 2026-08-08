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
package features.gateway.funs.exchange;

import features.gateway.funs.ExContextEmpty;
import io.vertx.core.MultiMap;
import io.vertx.core.net.SocketAddress;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.XForwardedHeaders;
import org.noear.solon.cloud.gateway.properties.XForwardedProperties;
import org.noear.solon.test.SolonTest;

import java.net.URI;

/**
 * XForwardedHeaders 出站头应用工具单测
 *
 * @author noear
 * @since 4.0.5
 */
@SolonTest
public class XForwardedHeadersTest {

    /** 构造一个带完整请求信息的 ExContext 匿名实现 */
    private ExContext buildCtx(SocketAddress remote, String host, URI uri, boolean secure) {
        return new ExContextEmpty() {
            @Override
            public SocketAddress remoteAddress() {
                return remote;
            }

            @Override
            public String rawHeader(String key) {
                return host;
            }

            @Override
            public URI rawURI() {
                return uri;
            }

            @Override
            public boolean isSecure() {
                return secure;
            }
        };
    }

    @Test
    public void apply_nullProps_noOp() {
        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(null, new ExContextEmpty(), headers);
        Assertions.assertTrue(headers.isEmpty());
    }

    @Test
    public void apply_disabled_noOp() {
        XForwardedProperties props = new XForwardedProperties();
        props.setEnabled(false);

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, new ExContextEmpty(), headers);
        Assertions.assertTrue(headers.isEmpty());
    }

    @Test
    public void apply_defaults_allHeaders() {
        // 默认配置：for 追加、host/port/proto 覆盖
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = buildCtx(
                SocketAddress.inetSocketAddress(8080, "192.168.1.5"),
                "example.com",
                URI.create("http://example.com:8080/path?a=1"),
                false);

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertEquals("192.168.1.5", headers.get("X-Forwarded-For"));
        Assertions.assertEquals("example.com", headers.get("X-Forwarded-Host"));
        Assertions.assertEquals("8080", headers.get("X-Forwarded-Port"));
        Assertions.assertEquals("http", headers.get("X-Forwarded-Proto"));
    }

    @Test
    public void apply_forAppend_existingValue() {
        // forAppend=true 且已有值 -> 追加
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = buildCtx(
                SocketAddress.inetSocketAddress(8080, "192.168.1.5"),
                "example.com",
                URI.create("http://example.com:8080/"),
                false);

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        headers.add("X-Forwarded-For", "1.2.3.4");
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertEquals("1.2.3.4, 192.168.1.5", headers.get("X-Forwarded-For"));
    }

    @Test
    public void apply_remoteAddressNull_skipFor() {
        // remoteAddress 为 null -> 不生成 X-Forwarded-For
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = buildCtx(null, "example.com", URI.create("http://example.com/"), false);

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertNull(headers.get("X-Forwarded-For"));
        Assertions.assertEquals("example.com", headers.get("X-Forwarded-Host"));
    }

    @Test
    public void apply_hostEmpty_skipHost() {
        // Host 头为空 -> 不生成 X-Forwarded-Host
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = new ExContextEmpty() {
            @Override
            public SocketAddress remoteAddress() {
                return SocketAddress.inetSocketAddress(8080, "192.168.1.5");
            }

            @Override
            public String rawHeader(String key) {
                return "";
            }

            @Override
            public URI rawURI() {
                return URI.create("http://example.com:8080/");
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertNull(headers.get("X-Forwarded-Host"));
        Assertions.assertEquals("192.168.1.5", headers.get("X-Forwarded-For"));
    }

    @Test
    public void apply_uriNull_portFallback80() {
        // rawURI 为 null 且非安全 -> 端口回退 80
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = new ExContextEmpty() {
            @Override
            public SocketAddress remoteAddress() {
                return SocketAddress.inetSocketAddress(8080, "192.168.1.5");
            }

            @Override
            public String rawHeader(String key) {
                return "example.com";
            }

            @Override
            public URI rawURI() {
                return null;
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertEquals("80", headers.get("X-Forwarded-Port"));
        Assertions.assertEquals("http", headers.get("X-Forwarded-Proto"));
    }

    @Test
    public void apply_uriNull_secure_fallback443() {
        // rawURI 为 null 且安全 -> 端口回退 443 + proto=https
        XForwardedProperties props = new XForwardedProperties();

        ExContext ctx = new ExContextEmpty() {
            @Override
            public SocketAddress remoteAddress() {
                return SocketAddress.inetSocketAddress(8443, "192.168.1.5");
            }

            @Override
            public String rawHeader(String key) {
                return "example.com";
            }

            @Override
            public URI rawURI() {
                return null;
            }

            @Override
            public boolean isSecure() {
                return true;
            }
        };

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertEquals("443", headers.get("X-Forwarded-Port"));
        Assertions.assertEquals("https", headers.get("X-Forwarded-Proto"));
    }

    @Test
    public void apply_append_currentEmpty_set() {
        // append=true 但当前无值 -> 直接 set（不追加分隔符）
        XForwardedProperties props = new XForwardedProperties();
        props.setHostAppend(true);

        ExContext ctx = buildCtx(
                SocketAddress.inetSocketAddress(8080, "192.168.1.5"),
                "example.com",
                URI.create("http://example.com:8080/"),
                false);

        MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        XForwardedHeaders.apply(props, ctx, headers);

        Assertions.assertEquals("example.com", headers.get("X-Forwarded-Host"));
    }
}
