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
package org.noear.solon.cloud.gateway.properties;

/**
 * Http 客户端配置（超时 + 连接池 + WebSocket），绑定 solon.cloud.gateway.httpClient.*
 *
 * <ul>
 *   <li>超时（秒，继承 {@link TimeoutProperties}）：connectTimeout 连接建立；requestTimeout 等待响应头；
 *       responseTimeout 整体完成兜底</li>
 *   <li>连接池（{@link HttpPoolProperties}，QPS 适配公式见其类注释）</li>
 *   <li>WebSocket（{@link WebSocketProperties}）</li>
 *   <li>compression：出站 GZip 压缩</li>
 *   <li>proxy（{@link HttpProxyProperties}）：企业出网代理（HTTP/SOCKS），含 nonProxyHostsPattern 白名单直连</li>
 *   <li>ssl（{@link HttpSslProperties}）：上游 mTLS 客户端证书 / 自定义信任库</li>
 * </ul>
 *
 * @author noear
 * @since 4.0.5
 */
public class HttpClientProperties extends TimeoutProperties {
    private boolean compression = false;              //出站 GZip 压缩
    private HttpPoolProperties pool = new HttpPoolProperties();
    private WebSocketProperties websocket = new WebSocketProperties();
    private HttpProxyProperties proxy = new HttpProxyProperties();
    private HttpSslProperties ssl = new HttpSslProperties();

    public HttpClientProperties() {

    }

    /**
     * 获取连接池配置
     */
    public HttpPoolProperties getPool() {
        return pool;
    }

    public void setPool(HttpPoolProperties pool) {
        this.pool = pool;
    }

    /**
     * 获取 WebSocket 配置
     */
    public WebSocketProperties getWebsocket() {
        return websocket;
    }

    public void setWebsocket(WebSocketProperties websocket) {
        this.websocket = websocket;
    }

    /**
     * 是否启用出站 GZip 压缩
     */
    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
    }

    /**
     * 获取代理配置
     */
    public HttpProxyProperties getProxy() {
        return proxy;
    }

    public void setProxy(HttpProxyProperties proxy) {
        this.proxy = proxy;
    }

    /**
     * 获取 SSL 配置
     */
    public HttpSslProperties getSsl() {
        return ssl;
    }

    public void setSsl(HttpSslProperties ssl) {
        this.ssl = ssl;
    }
}
