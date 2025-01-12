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
package org.noear.solon.cloud.extend.mqtt.event;

import org.eclipse.paho.client.mqttv3.IMqttToken;

import java.io.Serializable;

/**
 * @author noear
 * @since 2.5
 */
public class MqttDeliveryCompleteEvent implements Serializable {
    private String clientId;
    private int messageId;
    private transient IMqttToken token;
    public MqttDeliveryCompleteEvent(String clientId, int messageId, IMqttToken token){
        this.clientId = clientId;
        this.messageId = messageId;
        this.token = token;
    }

    public int getMessageId() {
        return messageId;
    }

    public String getClientId() {
        return clientId;
    }

    public IMqttToken getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "MqttDeliveryCompleteEvent{" +
                "clientId='" + clientId + '\'' +
                ", messageId=" + messageId +
                '}';
    }
}
