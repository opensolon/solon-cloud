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
package org.noear.solon.cloud.extend.local.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.local.LocalProps;
import org.noear.solon.cloud.extend.local.impl.event.EventRunnable;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.noear.solon.cloud.utils.ExpirationUtils;
import org.noear.solon.core.event.EventBus;
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 云端事件（本地摸拟实现。不支持ACK，不支持延时。最好还是引入消息队列的适配框架）
 *
 * @author noear
 * @since 1.11
 */
public class CloudEventServiceLocalImpl implements CloudEventServicePlus {
    static final Logger log = LoggerFactory.getLogger(CloudEventServiceLocalImpl.class);

    private CloudProps cloudProps;

    public CloudEventServiceLocalImpl(CloudProps cloudProps) {
        this.cloudProps = cloudProps;
    }

    @Override
    public boolean publish(Event event) throws CloudEventException {
        if (Utils.isEmpty(event.topic())) {
            throw new IllegalArgumentException("Event missing topic");
        }

        if (Utils.isEmpty(event.content())) {
            throw new IllegalArgumentException("Event missing content");
        }

        long scheduled_millis = 0L;
        if (event.scheduled() != null) {
            scheduled_millis = event.scheduled().getTime() - System.currentTimeMillis();
        }

        if (scheduled_millis > 0L) {
            //延迟执行
            RunUtil.delay(new EventRunnable(this, event), scheduled_millis);
        } else {
            //异步执行
            RunUtil.async(() -> {
                try {
                    //派发
                    distribute(event);
                } catch (Throwable e) {
                    log.warn(e.getMessage(), e);
                }
            });
        }

        return true;
    }

    public void distribute(Event event) throws Throwable {
        //new topic
        String topicNew;
        if (Utils.isEmpty(event.group())) {
            topicNew = event.topic();
        } else {
            topicNew = event.group() + LocalProps.GROUP_TOPIC_SPLIT_MART + event.topic();
        }

        boolean isOk = false;

        try {
            CloudEventHandler eventHandler = observerManger.getByTopic(topicNew);
            if (eventHandler != null) {
                isOk = eventHandler.handle(event);
            } else {//只需要记录一下
                log.warn("There is no observer for this event topic[{}]", event.topic());
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }

        if (isOk == false) {
            //失败后，重新进入延时队列
            event.times(event.times() + 1);
            RunUtil.delay(new EventRunnable(this, event), ExpirationUtils.getExpiration(event.times()));
        }
    }

    private CloudEventObserverManger observerManger = new CloudEventObserverManger();

    @Override
    public void attention(EventLevel level, String channel, String group, String topic, String tag, int qos, CloudEventHandler observer) {
        //new topic
        String topicNew;
        if (Utils.isEmpty(group)) {
            topicNew = topic;
        } else {
            topicNew = group + LocalProps.GROUP_TOPIC_SPLIT_MART + topic;
        }

        observerManger.add(topicNew, level, group, topic, tag, qos, observer);
    }

    private String channel;
    private String group;

    @Override
    public String getChannel() {
        if (channel == null) {
            channel = cloudProps.getEventChannel();
        }
        return channel;
    }

    @Override
    public String getGroup() {
        if (group == null) {
            group = cloudProps.getEventGroup();
        }

        return group;
    }
}
