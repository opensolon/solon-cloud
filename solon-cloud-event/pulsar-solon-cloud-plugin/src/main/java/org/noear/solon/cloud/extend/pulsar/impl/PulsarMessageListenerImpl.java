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
package org.noear.solon.cloud.extend.pulsar.impl;

import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageListener;
import org.noear.snack.ONode;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.pulsar.PulsarProps;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.utils.ExpirationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author noear
 * @since 1.5
 */
public class PulsarMessageListenerImpl implements MessageListener<byte[]> {
    static final Logger log = LoggerFactory.getLogger(PulsarMessageListenerImpl.class);

    private CloudEventObserverManger observerManger;
    private String eventChannelName;

    public PulsarMessageListenerImpl(CloudProps cloudProps, CloudEventObserverManger observerManger) {
        this.observerManger = observerManger;
        this.eventChannelName = cloudProps.getEventChannel();
    }

    @Override
    public void received(Consumer<byte[]> consumer, Message<byte[]> msg) {
        try {
            String event_json = new String(msg.getValue());
            Event event = ONode.deserialize(event_json, Event.class);
            event.channel(eventChannelName);

            boolean isOk = onReceive(event); //吃掉异常，方便下面的动作

            if (isOk == false) {
                event.times(event.times() + 1);

                consumer.reconsumeLater(msg, ExpirationUtils.getExpiration(event.times()), TimeUnit.SECONDS);
            } else {
                consumer.acknowledge(msg);
            }
        } catch (Throwable e) {
            e = Utils.throwableUnwrap(e);

            log.warn(e.getMessage(), e); //todo: ?

            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void reachedEndOfTopic(Consumer<byte[]> consumer) {
        MessageListener.super.reachedEndOfTopic(consumer);
    }

    private boolean onReceive(Event event) {
        try {
            return onReceiveDo(event);
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理接收事件
     */
    private boolean onReceiveDo(Event event) throws Throwable {
        boolean isOk = true;
        CloudEventHandler handler = null;

        //new topic
        String topicNew;
        if (Utils.isEmpty(event.group())) {
            topicNew = event.topic();
        } else {
            topicNew = event.group() + PulsarProps.GROUP_SPLIT_MARK + event.topic();
        }

        handler = observerManger.getByTopic(topicNew);
        if (handler != null) {
            isOk = handler.handle(event);
        } else {
            //只需要记录一下
            log.warn("There is no observer for this event topic[{}]", topicNew);
        }

        return isOk;
    }
}
