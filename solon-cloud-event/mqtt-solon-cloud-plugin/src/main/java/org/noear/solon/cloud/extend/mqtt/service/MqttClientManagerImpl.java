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
package org.noear.solon.cloud.extend.mqtt.service;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.mqtt.event.MqttDeliveryCompleteEvent;
import org.noear.solon.cloud.model.EventObserver;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mqtt 客户端管理器实现
 *
 * @author noear
 * @since 2.5
 */
public class MqttClientManagerImpl implements MqttClientManager, MqttCallbackExtended {
    private static final Logger log = LoggerFactory.getLogger(MqttClientManagerImpl.class);
    private static final String PROP_EVENT_clientId = "event.clientId";

    private final String server;
    private final String username;
    private final String password;
    private final CloudEventObserverManger observerManger;
    private final String eventChannelName;

    private final MqttConnectOptions options;

    private String clientId;
    private boolean async = true;

    private final Set<ConnectCallback> connectCallbacks = Collections.synchronizedSet(new HashSet<>());


    public MqttClientManagerImpl(CloudEventObserverManger observerManger, CloudProps cloudProps) {
        this.observerManger = observerManger;
        this.eventChannelName = cloudProps.getEventChannel();
        this.server = cloudProps.getEventServer();
        this.username = cloudProps.getUsername();
        this.password = cloudProps.getPassword();
        this.clientId = cloudProps.getValue(PROP_EVENT_clientId);
        if (Utils.isEmpty(this.clientId)) {
            this.clientId = Solon.cfg().appName() + "-" + Utils.guid();
        }

        this.options = new MqttConnectOptions();

        if (Utils.isNotEmpty(username)) {
            options.setUserName(username);
        } else {
            options.setUserName(Solon.cfg().appName());
        }

        if (Utils.isNotEmpty(password)) {
            options.setPassword(password.toCharArray());
        }


        //设置死信
        options.setWill("client.close", clientId.getBytes(StandardCharsets.UTF_8), 1, false);


        options.setConnectionTimeout(30); //超时时长
        options.setKeepAliveInterval(20); //心跳时长，秒
        options.setServerURIs(new String[]{server});
        options.setCleanSession(false);
        options.setAutomaticReconnect(true);
        options.setMaxInflight(128); //根据情况再调整

        //绑定定制属性
        Properties props = cloudProps.getEventClientProps();
        if (props.size() > 0) {
            Utils.injectProperties(options, props);
        }

        //支持事件总线扩展
        EventBus.publish(options);
    }

    //在断开连接时调用
    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost, clientId={}", clientId, cause);

        if (options.isAutomaticReconnect() == false) {
            //如果不是自动重链，把客户端清掉（如果有自己重链，内部会自己处理）
            client = null;
        }
    }

    //已经预订的消息
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (log.isTraceEnabled()) {
            log.trace("MQTT message arrived, clientId={}, messageId={}", clientId, message.getId());
        }

        CloudEventHandler eventHandler = observerManger.getByTopic(topic);

        if (eventHandler != null) {
            MqttMessageHandler handler = new MqttMessageHandler(this, eventChannelName, eventHandler, topic, message);
            if (getAsync()) {
                RunUtil.async(handler);
            } else {
                handler.run();
            }
        }
    }

    //发布的 QoS 1 或 QoS 2 消息的传递令牌时调用
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        if (token.getMessageId() > 0) {
            if (log.isDebugEnabled()) {
                log.debug("MQTT message delivery completed, clientId={}, messageId={}", clientId, token.getMessageId());
            }

            EventBus.publish(new MqttDeliveryCompleteEvent(clientId, token.getMessageId(), token));
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        connectCallbacks.forEach(callback -> callback.connectComplete(reconnect));
    }


    private IMqttAsyncClient client;

    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    /**
     * 获取客户端
     */
    @Override
    public IMqttAsyncClient getClient() {
        if (client != null) {
            return client;
        }

        SYNC_LOCK.lock();
        try {
            if (client == null) {
                client = createClient();
            }

            return client;
        } finally {
            SYNC_LOCK.unlock();
        }
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setAsync(boolean async) {
        this.async = async;
    }

    @Override
    public boolean getAsync() {
        return async;
    }

    @Override
    public void addCallback(ConnectCallback connectCallback) {
        connectCallbacks.add(connectCallback);
    }

    @Override
    public boolean removeCallback(ConnectCallback connectCallback) {
        return connectCallbacks.remove(connectCallback);
    }

    /**
     * 创建客户端
     */
    private IMqttAsyncClient createClient() {
        try {
            client = new MqttAsyncClient(server, clientId, new MemoryPersistence());
            //设置手动ack
            client.setManualAcks(true);
            //设置默认回调
            client.setCallback(this);
            //转为毫秒
            long waitConnectionTimeout = options.getConnectionTimeout() * 1000;
            //开始链接
            client.connect(options).waitForCompletion(waitConnectionTimeout);

            //订阅
            subscribe();

            return client;
        } catch (MqttException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 订阅
     */
    private void subscribe() throws MqttException {
        if (observerManger.topicSize() < 1) {
            return;
        }

        String[] topicAry = observerManger.topicAll().toArray(new String[0]);
        int[] topicQos = new int[topicAry.length];
        IMqttMessageListener[] topicListener = new IMqttMessageListener[topicAry.length];
        for (int i = 0, len = topicQos.length; i < len; i++) {
            EventObserver eventObserver = observerManger.getByTopic(topicAry[i]);
            topicQos[i] = eventObserver.getQos();
            topicListener[i] = new MqttMessageListenerImpl(this, eventChannelName, eventObserver);
        }

        IMqttToken token = getClient().subscribe(topicAry, topicQos, topicListener);
        //转为毫秒
        long waitConnectionTimeout = options.getConnectionTimeout() * 1000;
        token.waitForCompletion(waitConnectionTimeout);
    }
}