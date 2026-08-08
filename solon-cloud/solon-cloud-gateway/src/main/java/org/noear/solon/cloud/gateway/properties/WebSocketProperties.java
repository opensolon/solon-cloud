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

import java.io.Serializable;

/**
 * WebSocket 客户端配置，绑定 solon.cloud.gateway.httpClient.websocket.*
 *
 * <ul>
 *   <li>maxConnections：并发连接上限（WS 长连接不占 HTTP 连接池，需独立控制）</li>
 *   <li>idleTimeout（秒）：连接空闲判定（无活动帧），半开连接兜底</li>
 *   <li>closingTimeout（秒）：关闭握手超时</li>
 *   <li>pingInterval（秒）：双向心跳间隔（防半开连接悬挂），&lt;=0 关闭心跳</li>
 * </ul>
 *
 * @author noear
 * @since 4.0.5
 */
public class WebSocketProperties implements Serializable {
    private int maxConnections = 200;    //WebSocket 最大连接数
    private int idleTimeout = 60;        //空闲超时（秒），无活动帧判定
    private int closingTimeout = 10;     //关闭超时（秒）
    private int pingInterval = 30;       //心跳间隔（秒），<=0 关闭心跳

    public WebSocketProperties() {

    }

    /**
     * 获取 WebSocket 最大连接数
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * 获取空闲超时（秒）
     */
    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    /**
     * 获取 WebSocket 关闭超时（秒）
     */
    public int getClosingTimeout() {
        return closingTimeout;
    }

    public void setClosingTimeout(int closingTimeout) {
        this.closingTimeout = closingTimeout;
    }

    /**
     * 获取 WebSocket 心跳间隔（秒）
     */
    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }
}
