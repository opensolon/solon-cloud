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
package org.noear.solon.cloud.extend.jedis.impl;

import org.noear.snack.ONode;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.jedis.JedisProps;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPubSub;

/**
 * @author noear
 * @since 1.10
 */
public class JedisEventConsumer extends JedisPubSub {
    static final Logger log = LoggerFactory.getLogger(JedisEventConsumer.class);

    CloudEventObserverManger observerManger;
    String eventChannelName;

    public JedisEventConsumer(CloudProps cloudProps, CloudEventObserverManger observerManger) {
        this.observerManger = observerManger;
        this.eventChannelName = cloudProps.getEventChannel();
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            Event event = ONode.deserialize(message, Event.class);
            event.channel(eventChannelName);

            onReceive(event);
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
            topicNew = event.group() + JedisProps.GROUP_SPLIT_MARK + event.topic();
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
