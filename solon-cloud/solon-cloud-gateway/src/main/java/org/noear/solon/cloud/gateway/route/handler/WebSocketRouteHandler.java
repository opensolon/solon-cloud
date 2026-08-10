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

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.exchange.ExConstants;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExContextImpl;
import org.noear.solon.cloud.gateway.exchange.XForwardedHeaders;
import org.noear.solon.cloud.gateway.properties.HttpClientProperties;
import org.noear.solon.cloud.gateway.properties.TimeoutProperties;
import org.noear.solon.cloud.gateway.properties.XForwardedProperties;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.rx.Completable;
import org.noear.solon.rx.CompletableEmitter;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.core.util.KeyValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * WebSocket 路由处理器
 *
 * @author stephondng
 * @since 3.7.1
 */
public class WebSocketRouteHandler implements RouteHandler {
    static final Logger log = LoggerFactory.getLogger(WebSocketRouteHandler.class);

    /**
     * 握手请求不透传的头
     *
     * <p>逐跳头 + 由 Vert.x WebSocketClient 按目标重新生成的握手头（Host / Sec-WebSocket-*），
     * 其余客户端头（Authorization、Cookie、自定义追踪/租户头等）原样透传；
     * 子协议经 subProtocols 单独协商，不走 header。</p>
     */
    private static final Set<String> SKIP_HANDSHAKE_HEADERS = new HashSet<>(Arrays.asList(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "proxy-connection",
            "host",
            "content-length",
            "sec-websocket-key",
            "sec-websocket-version",
            "sec-websocket-extensions",
            "sec-websocket-protocol",
            "sec-websocket-accept"));

    private final Vertx vertx;
    private final HttpClientProperties httpClientProps;
    private final XForwardedProperties xForwardedProps;
    private final int pingInterval;

    private WebSocketClient webSocketClient;

    public WebSocketRouteHandler(Vertx vertx) {
        this(vertx, new HttpClientProperties(), new XForwardedProperties());
    }

    public WebSocketRouteHandler(Vertx vertx, HttpClientProperties httpClientProps) {
        this(vertx, httpClientProps, new XForwardedProperties());
    }

    public WebSocketRouteHandler(Vertx vertx, HttpClientProperties httpClientProps, XForwardedProperties xForwardedProps) {
        this.vertx = vertx;
        this.httpClientProps = httpClientProps;
        this.xForwardedProps = xForwardedProps;
        this.pingInterval = httpClientProps.getWebsocket().getPingInterval();

        WebSocketClientOptions options = new WebSocketClientOptions()
                .setIdleTimeout(httpClientProps.getWebsocket().getIdleTimeout()) // seconds
                .setClosingTimeout(httpClientProps.getWebsocket().getClosingTimeout()) // seconds
                .setMaxConnections(httpClientProps.getWebsocket().getMaxConnections());
        //代理 + SSL（compression 为 HTTP GZip 不适用 WS；WS 客户端为全局单例，nonProxyHostsPattern 不生效）
        ClientOptionsUtil.applyWebSocket(httpClientProps, options);

        this.webSocketClient = vertx.createWebSocketClient(options);
    }

    @Override
    public String[] schemas() {
        return new String[]{"ws", "wss"};
    }

    /**
     * 处理 WebSocket 连接
     */
    @Override
    public Completable handle(ExContext ctx) {
        try {
            ctx.pause();

            // 构建 WebSocket 连接请求
            Future<WebSocket> wsFuture = buildWebSocketRequest(ctx);

            return Completable.create(emitter -> {
                wsFuture.onComplete(ar -> {
                    if (ar.succeeded()) {
                        handleWebSocketConnection(ctx, ar.result(), emitter);
                    } else {
                        emitter.onError(ar.cause());
                    }
                });
            });
        } catch (Throwable ex) {
            if (ex instanceof StatusException) {
                return Completable.error(ex);
            } else {
                return Completable.error(new StatusException(ex, 400));
            }
        }
    }

    /**
     * 构建 WebSocket 请求
     */
    private Future<WebSocket> buildWebSocketRequest(ExContext ctx) {
        // 配置绝对地址
        String targetUri = ctx.targetNew().toString() + ctx.newRequest().getPathAndQueryString();

        // 构建 WebSocket 选项
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setAbsoluteURI(targetUri)
                .setMethod(HttpMethod.GET);

        // 配置超时（无显式配置时使用全局默认；requestTimeout 为握手等待超时）
        TimeoutProperties timeout = ctx.timeout();
        if (timeout == null) {
            timeout = httpClientProps;
        }

        //透传客户端头（剥离逐跳头与由客户端重新生成的握手头），
        //使 Authorization/Cookie/自定义头以及 filter 对 newRequest 的头改写在 WS 路径同样生效
        HeadersMultiMap headers = new HeadersMultiMap();
        for (KeyValues<String> kv : ctx.newRequest().getHeaders()) {
            if (SKIP_HANDSHAKE_HEADERS.contains(kv.getKey().toLowerCase())) {
                continue;
            }

            headers.add(kv.getKey(), Arrays.asList(kv.getValues()));
        }
        options.setHeaders(headers);

        //子协议协商：经 subProtocols 表达（Vert.x 会据此重新生成 Sec-WebSocket-Protocol 头）
        String subProtocols = ctx.rawHeader(ExConstants.Sec_WebSocket_Protocol);
        if (Utils.isNotEmpty(subProtocols)) {
            for (String sp : subProtocols.split(",")) {
                String tmp = sp.trim();
                if (Utils.isNotEmpty(tmp)) {
                    options.addSubProtocol(tmp);
                }
            }
        }

        options.setConnectTimeout(timeout.getConnectTimeout() * 1000);
        options.setTimeout(timeout.getRequestTimeout() * 1000);

        //X-Forwarded-* 出站头统一生成（For/Host/Port/Proto）
        XForwardedHeaders.apply(xForwardedProps, ctx, options.getHeaders());

        return webSocketClient.connect(options);
    }

    /**
     * 处理 WebSocket 连接
     */
    private void handleWebSocketConnection(ExContext ctx, WebSocket targetWebSocket, CompletableEmitter emitter) {
        try {
            // 获取原始连接的 WebSocket
            ExContextImpl ctxImpl = (ExContextImpl) ctx;
            HttpServerRequest rawRequest = ctxImpl.rawRequest();

            // 升级当前请求为 WebSocket（会自动转发 header）
            Future<ServerWebSocket> sourceWebSocketFuture = rawRequest.toWebSocket();

            sourceWebSocketFuture.onComplete(sourceAr -> {
                if (sourceAr.succeeded()) {
                    ServerWebSocket sourceWebSocket = sourceAr.result();

                    // 双向转发消息
                    setupWebSocketForwarding(ctx, sourceWebSocket, targetWebSocket);

                    emitter.onComplete();
                } else {
                    //下游升级失败：立即关闭已建立的上游连接（否则只能等 idleTimeout 回收）
                    closeQuietly(targetWebSocket);
                    emitter.onError(sourceAr.cause());
                }
            });

        } catch (Throwable ex) {
            closeQuietly(targetWebSocket);
            emitter.onError(ex);
        }
    }

    /**
     * 静默关闭 WebSocket（清理路径，异常不外抛）
     */
    private void closeQuietly(WebSocket ws) {
        try {
            if (ws != null && !ws.isClosed()) {
                ws.close();
            }
        } catch (Throwable ex) {
            log.debug("Gateway target WS close failed", ex);
        }
    }

    /**
     * 设置 WebSocket 消息双向转发和关闭/错误处理
     */
    private void setupWebSocketForwarding(ExContext ctx, ServerWebSocket clientWS, WebSocket targetWS) {
        // 心跳定时器引用（先声明，closeHandler 中统一取消；Vert.x closeHandler 为覆盖语义，只能注册一次）
        long[] timerRef = {-1L};

        // 使用 frameHandler 处理，以确保转发所有类型的帧（文本、二进制、Ping/Pong等）
        clientWS.frameHandler(frame -> {
            if (!targetWS.isClosed()) {
                targetWS.writeFrame(frame);
            }
        });

        // 客户端连接关闭时：取消心跳 + 关闭目标连接（合并进同一个 closeHandler，避免重复注册覆盖）
        clientWS.closeHandler(v -> {
            if (timerRef[0] != -1L) {
                ctx.vertx().cancelTimer(timerRef[0]);
            }

            if (!targetWS.isClosed()) {
                // 可以发送一个 close frame 到目标服务器
                targetWS.close();
            }
        });

        // 客户端连接错误时，关闭目标连接
        clientWS.exceptionHandler(error -> {
            log.warn("Client WS Error: " + error.getMessage());

            if (!targetWS.isClosed()) {
                targetWS.close();
            }
        });

        // ----------------------------------------------------
        // 2. 目标服务器 (targetWS) 到 客户端 (clientWS) 转发
        // ----------------------------------------------------

        // 使用 frameHandler 处理所有类型的帧
        targetWS.frameHandler(frame -> {
            if (!clientWS.isClosed()) {
                clientWS.writeFrame(frame);
            }
        });

        // 目标连接关闭时：取消心跳 + 关闭客户端连接（合并进同一个 closeHandler，避免重复注册覆盖）
        targetWS.closeHandler(v -> {
            if (timerRef[0] != -1L) {
                ctx.vertx().cancelTimer(timerRef[0]);
            }

            if (!clientWS.isClosed()) {
                // 可以发送一个 close frame 到客户端
                clientWS.close();
            }
        });

        // 目标连接错误时，关闭客户端连接
        targetWS.exceptionHandler(error -> {
            log.warn("Target WS Error: " + error.getMessage());

            if (!clientWS.isClosed()) {
                clientWS.close();
            }
        });

        // ----------------------------------------------------
        // 3. 心跳保活（双向 ping），防半开连接悬挂；关闭时经 closeHandler 统一取消定时器
        // ----------------------------------------------------
        if (pingInterval > 0) {
            timerRef[0] = ctx.vertx().setPeriodic(pingInterval * 1000L, id -> {
                if (!clientWS.isClosed()) {
                    clientWS.writePing(Buffer.buffer("gateway-ping"), ar -> {
                    });
                }

                if (!targetWS.isClosed()) {
                    targetWS.writePing(Buffer.buffer("gateway-ping"), ar -> {
                    });
                }
            });
        }
    }
}
