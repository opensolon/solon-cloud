/*
 * Copyright 2017-2024 noear.org and authors
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
package org.noear.solon.cloud.extend.mqtt.service;

import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;

/**
 * Mqtt 客户端管理器（便于支持自动重连和触发式重连）
 *
 * @author noear
 * @since 2.5
 */
public interface MqttClientManager {
    /**
     * 获取客户端
     */
    IMqttAsyncClient getClient();

    /**
     * 获取客户端Id
     */
    String getClientId();

    /**
     * 设置异步状态
     */
    void setAsync(boolean async);

    /**
     * 获取异步状态
     */
    boolean getAsync();

    /**
     * 添加连接回调
     */
    void addCallback(ConnectCallback connectCallback);

    /**
     * 移除连接回调
     */
    boolean removeCallback(ConnectCallback connectCallback);

    @FunctionalInterface
    public interface ConnectCallback {
        void connectComplete(boolean isReconnect);
    }
}
