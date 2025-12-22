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
package org.noear.solon.cloud.extend.aliyun.ons.impl;

import com.aliyun.openservices.ons.api.Message;
import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author cgy
 * @since 1.11
 */
class MessageUtil {
    public static Message buildNewMessage(Event event, String topic) {
        String topicNew = topic.replace(".", "_");

        if (Utils.isEmpty(event.key())) {
            event.key(Utils.guid());
        }

        if (event.created() == 0L) {
            event.created(System.currentTimeMillis());
        }

        Message message = new Message(
                topicNew,
                event.tags(),
                event.key(),
                event.content().getBytes(StandardCharsets.UTF_8));

        //@since 3.1
        message.setBornTimestamp(event.created());

        //@since 3.0
        for (Map.Entry<String, String> kv : event.meta().entrySet()) {
            message.putUserProperties(kv.getKey(), kv.getValue());
        }

        if (event.scheduled() != null) {
            long delayTimestamp = event.scheduled().getTime() - System.currentTimeMillis();
            message.setStartDeliverTime(delayTimestamp);
        }
        return message;
    }
}
