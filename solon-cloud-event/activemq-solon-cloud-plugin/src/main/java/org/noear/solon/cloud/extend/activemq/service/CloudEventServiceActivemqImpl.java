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
package org.noear.solon.cloud.extend.activemq.service;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.activemq.ActivemqProps;
import org.noear.solon.cloud.extend.activemq.impl.ActivemqConsumer;
import org.noear.solon.cloud.extend.activemq.impl.ActivemqProducer;
import org.noear.solon.cloud.extend.activemq.impl.ActivemqTransactionListener;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.model.EventTran;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.noear.solon.core.bean.LifecycleBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author liuxuehua12
 * @since 2.0
 */
public class CloudEventServiceActivemqImpl implements CloudEventServicePlus, LifecycleBean {
    static Logger log = LoggerFactory.getLogger(CloudEventServiceActivemqImpl.class);
    private CloudProps cloudProps;
    private ActivemqProducer producer;
    private ActivemqConsumer consumer;

    public CloudEventServiceActivemqImpl(CloudProps cloudProps) {
        this.cloudProps = cloudProps;

        ActiveMQConnectionFactory factory = null;

        String brokerUrl = cloudProps.getEventServer();
        if (brokerUrl.indexOf("://") < 0) {
            brokerUrl = "tcp://" + brokerUrl;
        }

        String username = cloudProps.getUsername();
        String password = cloudProps.getPassword();
        if (Utils.isEmpty(cloudProps.getUsername())) {
            factory = new ActiveMQConnectionFactory(brokerUrl);
        } else {
            factory = new ActiveMQConnectionFactory(username, password, brokerUrl);
        }

        //增加自动重发策略
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setInitialRedeliveryDelay(5000);//5s
        redeliveryPolicy.setBackOffMultiplier(2);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setMaximumRedeliveries(-1);//不限次
        redeliveryPolicy.setMaximumRedeliveryDelay(1000 * 60 * 60 * 2);//2小时

        factory.setRedeliveryPolicy(redeliveryPolicy);

        producer = new ActivemqProducer(factory);
        consumer = new ActivemqConsumer(factory, producer);
    }

    private void beginTransaction(EventTran eventTran) throws CloudEventException {
        if (eventTran.getListener(ActivemqTransactionListener.class) != null) {
            return;
        }

        try {
            eventTran.setListener(new ActivemqTransactionListener(producer.beginTransaction()));
        } catch (Exception e) {
            throw new CloudEventException(e);
        }
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

        if (event.tran() != null) {
            //如果有事务
            beginTransaction(event.tran());
        }

        //new topic
        String topicNew = ActivemqProps.getTopicNew(event);
        try {
            boolean re = producer.publish(event, topicNew);
            return re;
        } catch (Throwable ex) {
            throw new CloudEventException(ex);
        }
    }

    CloudEventObserverManger observerManger = new CloudEventObserverManger();

    @Override
    public void attention(EventLevel level, String channel, String group,
                          String topic, String tag, int qos, CloudEventHandler observer) {
        //new topic
        String topicNew;
        if (Utils.isEmpty(group)) {
            topicNew = topic;
        } else {
            topicNew = group + ActivemqProps.GROUP_SPLIT_MARK + topic;
        }

        observerManger.add(topicNew, level, group, topic, tag, qos, observer);

    }

    @Override
    public void postStart() throws Throwable {
        subscribe();
    }

    private void subscribe() {
        if (observerManger.topicSize() > 0) {
            try {
                consumer.init(observerManger);
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
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
}