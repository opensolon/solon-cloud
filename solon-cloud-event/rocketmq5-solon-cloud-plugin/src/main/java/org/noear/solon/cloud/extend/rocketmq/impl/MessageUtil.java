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

import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.message.MessageBuilder;
import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author noear
 * @since 1.3
 */
class MessageUtil {
    public static Message buildNewMeaage(ClientServiceProvider producer, Event event, String topic) {
        String topicNew = topic.replace(".", "_");

        if (Utils.isEmpty(event.key())) {
            event.key(Utils.guid());
        }

        MessageBuilder messageBuilder = producer.newMessageBuilder();

        messageBuilder.setTopic(topicNew)
                //设置消息索引键，可根据关键字精确查找某条消息。
                .setKeys(event.key())
                //消息体。
                .setBody(event.content().getBytes(StandardCharsets.UTF_8));


        for(Map.Entry<String,String> kv: event.meta().entrySet()) {
            messageBuilder.addProperty(kv.getKey(), kv.getValue());
        }

        //设置消息Tag，用于消费端根据指定Tag过滤消息。
        if (Utils.isNotEmpty(event.tags())) {
            messageBuilder.setTag(event.tags());
        }

        //设置超时
        if (event.scheduled() != null) {
            long delayTimestamp = event.scheduled().getTime() - System.currentTimeMillis();
            messageBuilder.setDeliveryTimestamp(delayTimestamp);
        }

        return messageBuilder.build();

    }
}
