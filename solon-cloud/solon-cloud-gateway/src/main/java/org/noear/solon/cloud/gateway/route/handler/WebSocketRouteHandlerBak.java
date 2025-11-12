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
import io.vertx.core.http.*;
import io.vertx.solon.VertxHolder;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExContextImpl;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.rx.Completable;
import org.noear.solon.rx.CompletableEmitter;
import org.noear.solon.core.exception.StatusException;

/**
 * WebSocket 路由处理器
 *
 * @author stephondng
 * @since 3.7.1
 */
public class WebSocketRouteHandlerBak implements RouteHandler {
    private WebSocketClient webSocketClient;

    public WebSocketRouteHandlerBak() {
        WebSocketClientOptions options = new WebSocketClientOptions()
                .setConnectTimeout(1000 * 3) // milliseconds: 3s
                .setIdleTimeout(60) // seconds: 60s
                .setClosingTimeout(10) // seconds: 10s
                .setMaxConnections(200);
        //.setCompressionLevel(0);

        this.webSocketClient = VertxHolder.getVertx().createWebSocketClient(options);
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
                        emitter.onError(new StatusException("Failed to connect to target WebSocket: " + ar.cause().getMessage(), 503));
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

        // 配置超时
        if (ctx.timeout() != null) {
            options.setConnectTimeout(ctx.timeout().getConnectTimeout() * 1000);
            options.setTimeout(ctx.timeout().getResponseTimeout() * 1000);
        }

        // 转发客户端请求中的所有 Header
        ctx.newRequest().getHeaders().forEach(entry -> {
            // 排除 Connection 和 Upgrade，Vert.x 客户端会自动设置正确的握手 Header
            if (!HttpHeaders.CONNECTION.toString().equalsIgnoreCase(entry.getKey()) &&
                    !HttpHeaders.UPGRADE.toString().equalsIgnoreCase(entry.getKey())) {
                for (String val : entry.getValues()) {
                    options.addHeader(entry.getKey(), val);
                }
            }
        });

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

            // 升级当前请求为 WebSocket
            Future<ServerWebSocket> serverWebSocketFuture = rawRequest.toWebSocket();

            serverWebSocketFuture.onComplete(serverAr -> {
                if (serverAr.succeeded()) {
                    ServerWebSocket serverWebSocket = serverAr.result();

                    // 核心：设置双向转发消息和关闭事件
                    setupWebSocketForwarding(serverWebSocket, targetWebSocket);

                    emitter.onComplete();
                } else {
                    //客户端握手失败
                    targetWebSocket.close();
                    emitter.onError(serverAr.cause());
                }
            });

        } catch (Throwable ex) {
            targetWebSocket.close();
            emitter.onError(ex);
        }
    }


    /**
     * 设置 WebSocket 消息双向转发和关闭/错误处理
     */
    private void setupWebSocketForwarding(ServerWebSocket clientWS, WebSocket targetWS) {
        // 使用 frameHandler 处理，以确保转发所有类型的帧（文本、二进制、Ping/Pong等）
        clientWS.frameHandler(frame -> {
            if (!targetWS.isClosed()) {
                targetWS.writeFrame(frame);
            }
        });

        // 客户端连接关闭时，关闭目标连接
        clientWS.closeHandler(v -> {
            if (!targetWS.isClosed()) {
                // 可以发送一个 close frame 到目标服务器
                targetWS.close();
            }
        });

        // 客户端连接错误时，关闭目标连接
        clientWS.exceptionHandler(error -> {
            System.err.println("Client WS Error: " + error.getMessage());
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

        // 目标连接关闭时，关闭客户端连接
        targetWS.closeHandler(v -> {
            if (!clientWS.isClosed()) {
                // 可以发送一个 close frame 到客户端
                clientWS.close();
            }
        });

        // 目标连接错误时，关闭客户端连接
        targetWS.exceptionHandler(error -> {
            System.err.println("Target WS Error: " + error.getMessage());
            if (!clientWS.isClosed()) {
                clientWS.close();
            }
        });
    }
}