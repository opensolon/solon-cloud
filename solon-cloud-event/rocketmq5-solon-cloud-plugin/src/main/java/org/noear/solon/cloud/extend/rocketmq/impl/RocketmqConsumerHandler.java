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
package org.noear.solon.cloud.extend.rocketmq.impl;

import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.extend.rocketmq.RocketmqProps;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;


/**
 * @author noear
 * @since 1.3
 * @since 1.11
 */
public class RocketmqConsumerHandler implements MessageListener {
    static Logger log = LoggerFactory.getLogger(RocketmqConsumerHandler.class);

    private final CloudEventObserverManger observerManger;
    private final RocketmqConfig config;

    public RocketmqConsumerHandler(RocketmqConfig config, CloudEventObserverManger observerManger) {
        this.observerManger = observerManger;
        this.config = config;
    }


    @Override
    public ConsumeResult consume(MessageView message) {
        boolean isOk = true;

        try {
            String topicNew = message.getTopic();
            String group = null;
            String topic = null;
            if (topicNew.contains(RocketmqProps.GROUP_SPLIT_MARK)) {
                group = topicNew.split(RocketmqProps.GROUP_SPLIT_MARK)[0];
                topic = topicNew.split(RocketmqProps.GROUP_SPLIT_MARK)[1];
            } else {
                topic = topicNew;
            }

            String content = StandardCharsets.UTF_8.decode(message.getBody()).toString();

            Event event = new Event(topic, content);
            event.tags(message.getTag().orElse(null));
            event.key(String.join(",", message.getKeys()));
            event.times(message.getDeliveryAttempt() - 1); //它是从1开始的
            event.channel(config.getChannelName());

            if (Utils.isNotEmpty(group)) {
                event.group(group);
            }

            //@since 3.0
            if (Utils.isNotEmpty(message.getProperties())) {
                event.meta().putAll(message.getProperties());
            }

            isOk = isOk && onReceive(event, topicNew); //可以不吃异常

        } catch (Throwable e) {
            isOk = false;
            log.warn(e.getMessage(), e);
        }

        if (isOk) {
            return ConsumeResult.SUCCESS;
        } else {
            return ConsumeResult.FAILURE;
        }
    }

    /**
     * 处理接收事件
     */
    protected boolean onReceive(Event event, String topicNew) throws Throwable {
        boolean isOk = true;
        CloudEventHandler handler = null;

        if (Utils.isEmpty(event.tags())) {
            handler = observerManger.getByTopicAndTag(topicNew, "*");
        } else {
            handler = observerManger.getByTopicAndTag(topicNew, event.tags());

            if (handler == null) {
                handler = observerManger.getByTopicAndTag(topicNew, "*");
            }
        }

        if (handler != null) {
            isOk = handler.handle(event);
        } else {
            //只需要记录一下
            log.warn("There is no observer for this event topic[{}]", topicNew);
        }

        return isOk;
    }
}
