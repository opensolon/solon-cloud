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
package org.noear.solon.cloud.extend.folkmq.service;

import org.noear.folkmq.FolkMQ;
import org.noear.folkmq.client.*;
import org.noear.snack.ONode;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.folkmq.FolkmqProps;
import org.noear.solon.cloud.extend.folkmq.impl.FolkmqConsumeHandler;
import org.noear.solon.cloud.extend.folkmq.impl.FolkmqTransactionListener;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.model.EventObserver;
import org.noear.solon.cloud.model.EventTran;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.noear.solon.core.bean.LifecycleBean;
import org.noear.solon.core.event.EventBus;

import java.io.IOException;
import java.util.Map;

/**
 * @author noear
 * @since 2.6
 */
public class CloudEventServiceFolkMqImpl implements CloudEventServicePlus, LifecycleBean {
    protected final MqClient client;

    private final CloudProps cloudProps;
    private final FolkmqConsumeHandler folkmqConsumeHandler;
    private final CloudEventObserverManger observerManger;
    private final long publishTimeout;

    public MqClient getClient() {
        return client;
    }

    public CloudEventServiceFolkMqImpl(CloudProps cloudProps) {
        this.cloudProps = cloudProps;
        this.observerManger = new CloudEventObserverManger();
        this.folkmqConsumeHandler = new FolkmqConsumeHandler(observerManger);
        this.publishTimeout = cloudProps.getEventPublishTimeout();

        this.client = FolkMQ.createClient(cloudProps.getEventServer())
                .nameAs(Solon.cfg().appName())
                .namespaceAs(cloudProps.getNamespace())
                .autoAcknowledge(false);

        if (publishTimeout > 0) {
            client.config(c -> c.requestTimeout(publishTimeout));
        }

        //总线扩展
        EventBus.publish(client);

        try {
            client.connect();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }


    private void beginTransaction(EventTran transaction) {
        if (transaction.getListener(FolkmqTransactionListener.class) != null) {
            return;
        }

        transaction.setListener(new FolkmqTransactionListener(client.newTransaction()));
    }

    @Override
    public boolean publish(Event event) throws CloudEventException {
        if (Utils.isEmpty(event.topic())) {
            throw new IllegalArgumentException("Event missing topic");
        }

        if (Utils.isEmpty(event.content())) {
            throw new IllegalArgumentException("Event missing content");
        }

        if (event.tran() != null) {
            beginTransaction(event.tran());
        }

        //new topic
        String topicNew = FolkmqProps.getTopicNew(event);
        try {
            MqMessage message = new MqMessage(event.content(), event.key())
                    .scheduled(event.scheduled())
                    .broadcast(event.broadcast())
                    .tag(event.tags())
                    .qos(event.qos());


            if (Utils.isNotEmpty(event.meta())) {
                message.attr("event_meta", ONode.stringify(event.meta()));
            }

            if (event.tran() != null) {
                MqTransaction transaction = event.tran().getListener(FolkmqTransactionListener.class).getTransaction();
                message.transaction(transaction);
            }

            if (publishTimeout > 0) {
                //同步
                client.publish(topicNew, message);
            } else {
                //异步
                client.publishAsync(topicNew, message);
            }
        } catch (Throwable ex) {
            throw new CloudEventException(ex);
        }
        return true;
    }

    @Override
    public void attention(EventLevel level, String channel, String group,
                          String topic, String tag, int qos, CloudEventHandler observer) {
        //new topic
        String topicNew;
        if (Utils.isEmpty(group)) {
            topicNew = topic;
        } else {
            topicNew = group + FolkmqProps.GROUP_SPLIT_MARK + topic;
        }

        observerManger.add(topicNew, level, group, topic, tag, qos, observer);

    }

    @Override
    public void postStart() throws Throwable {
        subscribe();
    }

    private void subscribe() throws IOException {
        if (observerManger.topicSize() > 0) {
            Instance instance = Instance.local();

            for (String topicNew : observerManger.topicAll()) {
                EventObserver observer = observerManger.getByTopic(topicNew);

                if (observer.getLevel() == EventLevel.instance) {
                    String instanceName = Instance.local().serviceAndAddress()
                            .replace("@", "-")
                            .replace(".", "_")
                            .replace(":", "_");
                    //实例订阅
                    client.subscribe(topicNew, instanceName, folkmqConsumeHandler);
                } else {
                    //集群订阅
                    client.subscribe(topicNew, instance.service(), folkmqConsumeHandler);
                }
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