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

import org.apache.rocketmq.common.message.Message;
import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author noear
 * @since 1.3
 */
class MessageUtil {
    public static Message buildNewMeaage(Event event, String topic) {
        String topicNew = topic.replace(".", "_");

        if (Utils.isEmpty(event.key())) {
            event.key(Utils.guid());
        }

        Message message = new Message(
                topicNew,
                event.tags(),
                event.key(),
                event.content().getBytes(StandardCharsets.UTF_8));

        //@since 3.0
        for(Map.Entry<String,String> kv: event.meta().entrySet()) {
            message.putUserProperty(kv.getKey(), kv.getValue());
        }

        if (event.scheduled() != null) {
            long delaySeconds = (event.scheduled().getTime() - System.currentTimeMillis()) / 1000;
            double delayHours = delaySeconds / 60 / 60;


            if (delaySeconds > 0) {
                if (delayHours > 2) {
                    throw new IllegalArgumentException("RocketMQ does not support a delay longer than 2 hours");
                }

                if (delaySeconds < 5) {
                    message.setDelayTimeLevel(1);
                } else if (delaySeconds < 10) {
                    message.setDelayTimeLevel(2);
                } else if (delaySeconds < 30) {
                    message.setDelayTimeLevel(3);
                } else if (delaySeconds < 60) {
                    message.setDelayTimeLevel(4);
                } else if (delaySeconds < 60 * 2) {
                    message.setDelayTimeLevel(5);
                } else if (delaySeconds < 60 * 3) {
                    message.setDelayTimeLevel(6);
                } else if (delaySeconds < 60 * 4) {
                    message.setDelayTimeLevel(7);
                } else if (delaySeconds < 60 * 5) {
                    message.setDelayTimeLevel(8);
                } else if (delaySeconds < 60 * 6) {
                    message.setDelayTimeLevel(9);
                } else if (delaySeconds < 60 * 7) {
                    message.setDelayTimeLevel(10);
                } else if (delaySeconds < 60 * 8) {
                    message.setDelayTimeLevel(11);
                } else if (delaySeconds < 60 * 9) {
                    message.setDelayTimeLevel(12);
                } else if (delaySeconds < 60 * 10) {
                    message.setDelayTimeLevel(13);
                } else if (delaySeconds < 60 * 20) {
                    message.setDelayTimeLevel(14);
                } else if (delaySeconds < 60 * 30) {
                    message.setDelayTimeLevel(15);
                } else if (delaySeconds < 60 * 60) {
                    message.setDelayTimeLevel(16);
                } else if (delaySeconds < 60 * 120) {
                    message.setDelayTimeLevel(17);
                } else {
                    message.setDelayTimeLevel(18);
                }
            }
        }

        return message;
    }
}
