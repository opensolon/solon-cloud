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
package org.noear.solon.cloud.extend.mqtt5.service;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.core.util.RunUtil;

/**
 * 消息监听（与主题一对一）
 *
 * @author noear
 * @since 2.4
 */
public class MqttMessageListenerImpl implements IMqttMessageListener {
    private CloudEventHandler eventHandler;
    private String eventChannelName;
    private MqttClientManager clientManager;

    public MqttMessageListenerImpl(MqttClientManager clientManager, String eventChannelName, CloudEventHandler eventHandler) {
        this.eventHandler = eventHandler;
        this.eventChannelName = eventChannelName;
        this.clientManager = clientManager;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        MqttMessageHandler handler = new MqttMessageHandler(clientManager, eventChannelName, eventHandler, topic, message);

        if (clientManager.getAsync()) {
            RunUtil.parallel(handler);
        } else {
            handler.run();
        }
    }
}
