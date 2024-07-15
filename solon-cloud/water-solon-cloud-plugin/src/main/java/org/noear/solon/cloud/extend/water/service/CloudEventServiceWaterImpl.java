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
package org.noear.solon.cloud.extend.water.service;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.water.WaterProps;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.noear.water.WW;
import org.noear.water.WaterClient;
import org.noear.water.utils.EncryptUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 事件服务
 *
 * @author noear
 * @since 1.2
 */
public class CloudEventServiceWaterImpl implements CloudEventServicePlus {
    static Logger log = LoggerFactory.getLogger(CloudEventServiceWaterImpl.class);

    private final CloudProps cloudProps;

    private final String DEFAULT_SEAL = "Pckb6BpGzDE6RUIy";
    private String seal;
    private boolean unstable;
    private String eventChannelName;
    private String eventBroker;

    public CloudEventServiceWaterImpl(CloudProps cloudProps) {
        this.cloudProps = cloudProps;

        this.unstable = Solon.cfg().isFilesMode()
                || Solon.cfg().isDriftMode();

        this.eventChannelName = cloudProps.getEventChannel();
        this.eventBroker = cloudProps.getEventBroker();

        this.seal = getEventSeal();

        if (Utils.isEmpty(seal)) {
            seal = DEFAULT_SEAL;
        }
    }

    public String getSeal() {
        return seal;
    }

    @Override
    public boolean publish(Event event) throws CloudEventException {
        if (Utils.isEmpty(event.topic())) {
            throw new IllegalArgumentException("Event missing topic");
        }

        if (Utils.isEmpty(event.content())) {
            throw new IllegalArgumentException("Event missing content");
        }

        //new topic
        String topicNew;
        if (Utils.isEmpty(event.group())) {
            topicNew = event.topic();
        } else {
            topicNew = event.group() + WaterProps.GROUP_TOPIC_SPLIT_MART + event.topic();
        }

        try {
            return WaterClient.Message.
                    sendMessageAndTags(eventBroker, event.key(), topicNew, event.content(), event.scheduled(), event.tags());
        } catch (Throwable ex) {
            throw new CloudEventException(ex);
        }
    }


    private CloudEventObserverManger instanceObserverManger = new CloudEventObserverManger();
    private CloudEventObserverManger clusterObserverManger = new CloudEventObserverManger();

    /**
     * 登记关注
     */
    @Override
    public void attention(EventLevel level, String channel, String group, String topic, String tag, int qos, CloudEventHandler observer) {
        //new topic
        String topicNew;
        if (Utils.isEmpty(group)) {
            topicNew = topic;
        } else {
            topicNew = group + WaterProps.GROUP_TOPIC_SPLIT_MART + topic;
        }

        if (level == EventLevel.instance) {
            instanceObserverManger.add(topicNew, level, group, topic, tag, qos, observer);
        } else {
            clusterObserverManger.add(topicNew, level, group, topic, tag, qos, observer);
        }
    }

    /**
     * 执行订阅
     */
    public void subscribe() {
        try {
            subscribe0();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void subscribe0() throws Exception {
        Instance instance = Instance.local();

        if (instanceObserverManger.topicSize() > 0) {
            String instance_receiver_url = "http://" + instance.address() + WW.path_run_msg;
            String instance_subscriber_Key = EncryptUtils.md5(instance.service() + "_instance_" + instance_receiver_url);

            WaterClient.Message.subscribeTopic(eventBroker, instance_subscriber_Key,
                    instance.service(),
                    Solon.cfg().appGroup(),
                    instance_receiver_url,
                    seal,
                    "",
                    1,
                    unstable,
                    instanceObserverManger.topicAll().toArray(new String[instanceObserverManger.topicAll().size()]));
        }

        if (clusterObserverManger.topicSize() > 0) {

            String cluster_receiver_url;
            String cluster_hostname = getEventReceive();

            if (Utils.isEmpty(cluster_hostname)) {
                cluster_receiver_url = "@" + Solon.cfg().appName() +WW.path_run_msg;
            } else {
                if (cluster_hostname.indexOf("://") > 0) {
                    cluster_receiver_url = cluster_hostname + WW.path_run_msg;
                } else {
                    cluster_receiver_url = "http://" + cluster_hostname + WW.path_run_msg;
                }
            }

            String cluster_subscriber_Key = EncryptUtils.md5(instance.service() + "_cluster_" + cluster_receiver_url);

            WaterClient.Message.subscribeTopic(eventBroker, cluster_subscriber_Key,
                    instance.service(),
                    Solon.cfg().appGroup(),
                    cluster_receiver_url,
                    seal,
                    "",
                    1,
                    false,
                    clusterObserverManger.topicAll().toArray(new String[clusterObserverManger.topicSize()]));
        }
    }


    /**
     * 处理接收事件
     */
    public boolean onReceive(String topicNew, Event event) throws Throwable {
        boolean isOk = true; //不能改为 false；下面有 & 操作
        boolean isHandled = false;
        CloudEventHandler handler = null;

        event.channel(eventChannelName);

        handler = instanceObserverManger.getByTopic(topicNew);
        if (handler != null) {
            isHandled = true;
            isOk = handler.handle(event);
        }

        handler = clusterObserverManger.getByTopic(topicNew);
        if (handler != null) {
            isHandled = true;
            isOk = isOk && handler.handle(event); //两个都成功，才是成功
        }

        if (isHandled == false) {
            //只需要记录一下
            log.warn("There is no observer for this event topic[{}]", event.topic());
        }

        return isOk;
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


    public String getEventSeal() {
        return cloudProps.getValue(WaterProps.PROP_EVENT_seal);
    }

    public String getEventReceive() {
        return cloudProps.getValue(WaterProps.PROP_EVENT_receive);
    }
}
