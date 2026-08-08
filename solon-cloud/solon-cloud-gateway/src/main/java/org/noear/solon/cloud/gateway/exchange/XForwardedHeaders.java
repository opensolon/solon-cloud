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
package org.noear.solon.cloud.gateway.exchange;

import io.vertx.core.MultiMap;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.properties.XForwardedProperties;

/**
 * X-Forwarded-* 出站头应用工具
 *
 * <p>将客户端请求信息按 {@link XForwardedProperties} 策略生成到出站头集合
 * （HttpClientRequest.headers() / WebSocketConnectOptions.getHeaders()）。</p>
 *
 * @author noear
 * @since 4.0.5
 */
public class XForwardedHeaders {
    private XForwardedHeaders() {
    }

    /**
     * 应用 X-Forwarded-* 头到出站头集合
     *
     * @param props   配置（null 或 enabled=false 时不生成任何头）
     * @param ctx     交换上下文（原始请求信息源）
     * @param headers 出站头集合（可写 MultiMap）
     */
    public static void apply(XForwardedProperties props, ExContext ctx, MultiMap headers) {
        if (props == null || !props.isEnabled()) {
            return;
        }

        if (props.isForEnabled()) {
            String peerIp = ctx.remoteAddress() == null ? null : ctx.remoteAddress().host();
            if (Utils.isNotEmpty(peerIp)) {
                setOrAppend(headers, ExConstants.X_Forwarded_For, peerIp, props.isForAppend());
            }
        }

        if (props.isHostEnabled()) {
            String host = ctx.rawHeader(ExConstants.Host);
            if (Utils.isNotEmpty(host)) {
                setOrAppend(headers, ExConstants.X_Forwarded_Host, host, props.isHostAppend());
            }
        }

        if (props.isPortEnabled()) {
            int port = ctx.rawURI() == null ? 0 : ctx.rawURI().getPort();
            if (port <= 0) {
                port = ctx.isSecure() ? 443 : 80;
            }
            setOrAppend(headers, ExConstants.X_Forwarded_Port, String.valueOf(port), props.isPortAppend());
        }

        if (props.isProtoEnabled()) {
            setOrAppend(headers, ExConstants.X_Forwarded_Proto, ctx.isSecure() ? "https" : "http", props.isProtoAppend());
        }
    }

    private static void setOrAppend(MultiMap headers, String name, String value, boolean append) {
        if (append) {
            String current = headers.get(name);
            if (Utils.isNotEmpty(current)) {
                headers.set(name, current + ", " + value);
                return;
            }
        }

        headers.set(name, value);
    }
}
