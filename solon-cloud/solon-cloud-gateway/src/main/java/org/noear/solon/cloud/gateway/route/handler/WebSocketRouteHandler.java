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
public class WebSocketRouteHandler implements RouteHandler {
    private WebSocketClient webSocketClient;

    public WebSocketRouteHandler() {
        WebSocketClientOptions options = new WebSocketClientOptions()
                .setConnectTimeout(1000 * 3) // milliseconds: 3s
                .setIdleTimeout(60) // seconds: 60s
                .setClosingTimeout(10) // seconds: 10s
                .setMaxConnections(200);

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
        String targetUri = ctx.targetNew().toString();
        String pathAndQuery = ctx.newRequest().getPathAndQueryString();
        
        // 构建完整的 URI
        String fullUri = targetUri + pathAndQuery;
        
        // 构建 WebSocket 选项
        WebSocketConnectOptions options = new WebSocketConnectOptions()
                .setAbsoluteURI(fullUri)
                .setMethod(HttpMethod.GET);

        // 配置超时
        if (ctx.timeout() != null) {
            options.setConnectTimeout(ctx.timeout().getConnectTimeout() * 1000);
            options.setTimeout(ctx.timeout().getResponseTimeout() * 1000);
        }

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
            
            // 检查是否支持 WebSocket 升级
            if (!isWebSocketUpgradeRequest(rawRequest)) {
                emitter.onError(new StatusException("Not a WebSocket upgrade request", 400));
                return;
            }

            // 升级当前请求为 WebSocket
            Future<ServerWebSocket> serverWebSocketFuture = rawRequest.toWebSocket();
            
            serverWebSocketFuture.onComplete(serverAr -> {
                if (serverAr.succeeded()) {
                    ServerWebSocket serverWebSocket = serverAr.result();
                    
                    // 双向转发消息
                    setupWebSocketForwarding(serverWebSocket, targetWebSocket, emitter);
                    
                    // 设置响应头
                    ctx.newResponse().status(101); // Switching Protocols
                    ctx.newResponse().header("Upgrade", "websocket");
                    ctx.newResponse().header("Connection", "Upgrade");
                    
                    emitter.onComplete();
                } else {
                    emitter.onError(serverAr.cause());
                }
            });

        } catch (Throwable ex) {
            emitter.onError(ex);
        }
    }
    
    /**
     * 检查是否为 WebSocket 升级请求
     */
    private boolean isWebSocketUpgradeRequest(HttpServerRequest request) {
        String upgradeHeader = request.getHeader("Upgrade");
        String connectionHeader = request.getHeader("Connection");
        
        return "websocket".equalsIgnoreCase(upgradeHeader) && 
               connectionHeader != null && 
               connectionHeader.toLowerCase().contains("upgrade");
    }

    /**
     * 设置 WebSocket 消息双向转发
     */
    private void setupWebSocketForwarding(ServerWebSocket serverWebSocket, WebSocket targetWebSocket, CompletableEmitter emitter) {
        // 从客户端到目标服务器的消息转发
        serverWebSocket.textMessageHandler(message -> {
            if (!targetWebSocket.isClosed()) {
                targetWebSocket.writeTextMessage(message);
            }
        });

        serverWebSocket.binaryMessageHandler(message -> {
            if (!targetWebSocket.isClosed()) {
                targetWebSocket.writeBinaryMessage(message);
            }
        });

        // 从目标服务器到客户端的消息转发
        targetWebSocket.textMessageHandler(message -> {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.writeTextMessage(message);
            }
        });

        targetWebSocket.binaryMessageHandler(message -> {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.writeBinaryMessage(message);
            }
        });

        // 错误处理
        serverWebSocket.exceptionHandler(error -> {
            if (!targetWebSocket.isClosed()) {
                targetWebSocket.close();
            }
            emitter.onError(error);
        });

        targetWebSocket.exceptionHandler(error -> {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.close();
            }
            emitter.onError(error);
        });

        // 关闭处理
        serverWebSocket.closeHandler(v -> {
            if (!targetWebSocket.isClosed()) {
                targetWebSocket.close();
            }
        });

        targetWebSocket.closeHandler(v -> {
            if (!serverWebSocket.isClosed()) {
                serverWebSocket.close();
            }
        });
    }
}