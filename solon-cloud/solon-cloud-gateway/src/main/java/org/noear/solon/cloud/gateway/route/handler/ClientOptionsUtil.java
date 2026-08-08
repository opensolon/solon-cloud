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
package org.noear.solon.cloud.gateway.route.handler;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.properties.HttpClientProperties;
import org.noear.solon.cloud.gateway.properties.HttpProxyProperties;
import org.noear.solon.cloud.gateway.properties.HttpSslProperties;

/**
 * HttpClient 选项装配工具（compression / proxy / ssl），HTTP 与 WebSocket 客户端共用
 *
 * <p>注：Vert.x 4.5 中 {@link HttpClientOptions} 与 WebSocketClientOptions 同为
 * {@link ClientOptionsBase} 子类；compression 仅 HTTP 客户端适用（WebSocket 压缩走协议层
 * permessage-deflate，不属本组）。</p>
 *
 * @author noear
 * @since 4.0.5
 */
final class ClientOptionsUtil {
    private ClientOptionsUtil() {
    }

    /**
     * 应用压缩 + 代理 + SSL 选项（HTTP 客户端）
     */
    static void applyHttp(HttpClientProperties props, HttpClientOptions options) {
        options.setTryUseCompression(props.isCompression());
        applyProxy(props, options);
        applySsl(props, options);
    }

    /**
     * 应用代理 + SSL 选项（WebSocket 客户端；compression 为 HTTP GZip，不适用 WS）
     */
    static void applyWebSocket(HttpClientProperties props, ClientOptionsBase options) {
        applyProxy(props, options);
        applySsl(props, options);
    }

    /**
     * 应用代理选项（enabled + host 非空才装配）
     */
    static void applyProxy(HttpClientProperties props, ClientOptionsBase options) {
        HttpProxyProperties proxy = props.getProxy();
        if (proxy == null || !proxy.isEnabled() || Utils.isEmpty(proxy.getHost())) {
            return;
        }

        options.setProxyOptions(new ProxyOptions()
                .setHost(proxy.getHost())
                .setPort(proxy.getPort())
                .setType(proxyType(proxy.getType()))
                .setUsername(proxy.getUsername())
                .setPassword(proxy.getPassword()));
    }

    /**
     * 应用 SSL 选项（enabled 才装配；keyStore 缺省按 JKS，keyStoreType=PKCS12 时按 PKCS12）
     */
    static void applySsl(HttpClientProperties props, ClientOptionsBase options) {
        HttpSslProperties ssl = props.getSsl();
        if (ssl == null || !ssl.isEnabled()) {
            return;
        }

        options.setSsl(true);

        if (Utils.isNotEmpty(ssl.getKeyStore())) {
            if ("PKCS12".equalsIgnoreCase(ssl.getKeyStoreType())) {
                PfxOptions keyOpts = new PfxOptions()
                        .setPath(ssl.getKeyStore())
                        .setPassword(ssl.getKeyStorePassword());
                if (Utils.isNotEmpty(ssl.getKeyPassword())) {
                    //Vert.x 4.5 用 aliasPassword 表达密钥口令（缺省回退 password）
                    keyOpts.setAliasPassword(ssl.getKeyPassword());
                }
                options.setPfxKeyCertOptions(keyOpts);
            } else {
                JksOptions keyOpts = new JksOptions()
                        .setPath(ssl.getKeyStore())
                        .setPassword(ssl.getKeyStorePassword());
                if (Utils.isNotEmpty(ssl.getKeyPassword())) {
                    keyOpts.setAliasPassword(ssl.getKeyPassword());
                }
                options.setKeyStoreOptions(keyOpts);
            }
        }

        if (Utils.isNotEmpty(ssl.getTrustStore())) {
            if ("PKCS12".equalsIgnoreCase(ssl.getTrustStoreType())) {
                options.setPfxTrustOptions(new PfxOptions()
                        .setPath(ssl.getTrustStore())
                        .setPassword(ssl.getTrustStorePassword()));
            } else {
                options.setTrustStoreOptions(new JksOptions()
                        .setPath(ssl.getTrustStore())
                        .setPassword(ssl.getTrustStorePassword()));
            }
        }
    }

    /**
     * 代理类型解析（HTTP / SOCKS4 / SOCKS5，非法值回退 HTTP）
     */
    static ProxyType proxyType(String type) {
        if (type == null) {
            return ProxyType.HTTP;
        }

        switch (type.toUpperCase()) {
            case "SOCKS5":
                return ProxyType.SOCKS5;
            case "SOCKS4":
                return ProxyType.SOCKS4;
            default:
                return ProxyType.HTTP;
        }
    }
}
