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
package org.noear.solon.cloud.extend.pulsar.service;

import org.apache.pulsar.client.api.*;
import org.noear.snack.ONode;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.pulsar.PulsarProps;
import org.noear.solon.cloud.extend.pulsar.impl.PulsarMessageListenerImpl;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.model.EventTran;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * @author noear
 * @since 1.5
 */
public class CloudEventServicePulsarImpl implements CloudEventServicePlus {
    private static final Logger log = LoggerFactory.getLogger(CloudEventServicePulsarImpl.class);

    private static final String PROP_EVENT_consumerGroup = "event.consumerGroup";
    private static final String PROP_EVENT_producerGroup = "event.producerGroup";

    private final CloudProps cloudProps;

    private PulsarClient client;

    public CloudEventServicePulsarImpl(CloudProps cloudProps) {
        this.cloudProps = cloudProps;

        try {
            client = PulsarClient.builder()
                    .serviceUrl(cloudProps.getEventServer())
                    .build();
        } catch (PulsarClientException e) {
            throw new CloudEventException(e);
        }
    }

    private void beginTransaction(EventTran transaction) throws CloudEventException {
        //不支持事务消息
        log.warn("Event transactions are not supported!");
    }

    @Override
    public boolean publish(Event event) throws CloudEventException {
        if (Utils.isEmpty(event.topic())) {
            throw new IllegalArgumentException("Event missing topic");
        }

        if (Utils.isEmpty(event.content())) {
            throw new IllegalArgumentException("Event missing content");
        }

        if (Utils.isEmpty(event.key())) {
            event.key(Utils.guid());
        }

        if(event.tran() != null){
            beginTransaction(event.tran());
        }

        //new topic
        String topicNew;
        if (Utils.isEmpty(event.group())) {
            topicNew = event.topic();
        } else {
            topicNew = event.group() + PulsarProps.GROUP_SPLIT_MARK + event.topic();
        }

        byte[] event_data = ONode.stringify(event).getBytes(StandardCharsets.UTF_8);


        try (Producer<byte[]> producer = client.newProducer().topic(topicNew).create()) {

            if (event.scheduled() == null) {
                producer.newMessage()
                        .key(event.key())
                        .value(event_data)
                        .send();
            } else {
                producer.newMessage()
                        .key(event.key())
                        .value(event_data)
                        .deliverAt(event.scheduled().getTime())
                        .send();
            }

            return true;
        } catch (Throwable ex) {
            throw new CloudEventException(ex);
        }
    }

    CloudEventObserverManger observerManger = new CloudEventObserverManger();

    @Override
    public void attention(EventLevel level, String channel, String group, String topic, String tag, int qos, CloudEventHandler observer) {
        //new topic
        String topicNew;
        if (Utils.isEmpty(group)) {
            topicNew = topic;
        } else {
            topicNew = group + PulsarProps.GROUP_SPLIT_MARK + topic;
        }

        observerManger.add(topicNew, level, group, topic, tag, qos, observer);
    }

    public void subscribe() {
        if (observerManger.topicSize() > 0) {
            String consumerGroup = getEventConsumerGroup();

            if (Utils.isEmpty(consumerGroup)) {
                consumerGroup = Solon.cfg().appGroup() + "_" + Solon.cfg().appName();
            }

            try {
                client.newConsumer()
                        .topics(new ArrayList<>(observerManger.topicAll()))
                        .messageListener(new PulsarMessageListenerImpl(cloudProps, observerManger))
                        .subscriptionName(consumerGroup)
                        .subscriptionType(SubscriptionType.Shared)
                        .subscribe();
            } catch (Exception e) {
                throw new CloudEventException(e);
            }
        }
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

    /**
     * 消费组
     */
    public String getEventConsumerGroup() {
        return cloudProps.getValue(PROP_EVENT_consumerGroup);
    }

    /**
     * 产品组
     */
    public String getEventProducerGroup() {
        return cloudProps.getValue(PROP_EVENT_producerGroup);
    }
}
