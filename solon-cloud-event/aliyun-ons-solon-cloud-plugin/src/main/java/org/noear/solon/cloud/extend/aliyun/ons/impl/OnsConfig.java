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
package org.noear.solon.cloud.extend.aliyun.ons.impl;

import com.aliyun.openservices.ons.api.PropertyKeyConst;
import com.aliyun.openservices.ons.api.PropertyValueConst;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * @author cgy
 * @since 1.11
 */
public class OnsConfig {
    static final Logger log = LoggerFactory.getLogger(OnsConfig.class);

    private static final String PROP_EVENT_consumerGroup = "event.consumerGroup";
    private static final String PROP_EVENT_producerGroup = "event.producerGroup";

    private static final String PROP_EVENT_consumeThreadNums = "event.consumeThreadNums";
    private static final String PROP_EVENT_maxReconsumeTimes = "event.maxReconsumeTimes";

    private final String channelName;
    private final String server;

    private final String accessKey;
    private final String secretKey;

    private final long timeout;

    private String producerGroup;
    private String consumerGroup;


    //实例的消费线程数，0表示默认
    private final int consumeThreadNums;

    //设置消息消费失败的最大重试次数，0表示默认
    private final int maxReconsumeTimes;
    private final CloudProps cloudProps;

    public OnsConfig(CloudProps cloudProps) {
        this.cloudProps = cloudProps;

        server = cloudProps.getEventServer();
        channelName = cloudProps.getEventChannel();

        accessKey = cloudProps.getEventAccessKey();
        secretKey = cloudProps.getEventSecretKey();

        timeout = cloudProps.getEventPublishTimeout();

        consumeThreadNums = Integer.valueOf(cloudProps.getValue(PROP_EVENT_consumeThreadNums, "0"));
        maxReconsumeTimes = Integer.valueOf(cloudProps.getValue(PROP_EVENT_maxReconsumeTimes, "0"));

        producerGroup = cloudProps.getValue(PROP_EVENT_producerGroup);
        consumerGroup = cloudProps.getValue(PROP_EVENT_consumerGroup);

        if (Utils.isEmpty(producerGroup)) {
            producerGroup = "DEFAULT";
        }

        if (Utils.isEmpty(consumerGroup)) {
            consumerGroup = Solon.cfg().appGroup() + "_" + Solon.cfg().appName();
        }


        log.trace("producerGroup=" + producerGroup);
        log.trace("consumerGroup=" + consumerGroup);
    }

    public CloudProps getCloudProps() {
        return cloudProps;
    }

    public String getChannelName() {
        return channelName;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public Properties getProducerProperties() {
        Properties producer = getBaseProperties();
        producer.put(PropertyKeyConst.GROUP_ID, producerGroup);
        producer.put(PropertyKeyConst.SendMsgTimeoutMillis, timeout);
        return producer;
    }

    public Properties getConsumerProperties(String consumerGroupId) {
        Properties consumer = getBaseProperties();

        consumer.put(PropertyKeyConst.GROUP_ID, consumerGroupId);
        //只能是集群模式
        consumer.put(PropertyKeyConst.MessageModel, PropertyValueConst.CLUSTERING);
        //实例的消费线程数
        if (consumeThreadNums > 0) {
            consumer.put(PropertyKeyConst.ConsumeThreadNums, consumeThreadNums);
        }
        //设置消息消费失败的最大重试次数
        if (maxReconsumeTimes > 0) {
            consumer.put(PropertyKeyConst.MaxReconsumeTimes, maxReconsumeTimes);
        }
        return consumer;
    }

    public Properties getBaseProperties() {
        Properties properties = new Properties();

        if (Utils.isNotEmpty(accessKey)) {
            properties.put(PropertyKeyConst.AccessKey, accessKey);
        }
        if (Utils.isNotEmpty(secretKey)) {
            properties.put(PropertyKeyConst.SecretKey, secretKey);
        }

        properties.put(PropertyKeyConst.NAMESRV_ADDR, server);
        return properties;
    }
}
