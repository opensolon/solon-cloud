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

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear
 * @since 1.3
 * @since 1.11
 */
public class RocketmqConfig {
    static final Logger log = LoggerFactory.getLogger(RocketmqConfig.class);

    private static final String PROP_EVENT_consumerGroup = "event.consumerGroup";
    private static final String PROP_EVENT_producerGroup = "event.producerGroup";

    private static final String PROP_EVENT_consumeThreadNums = "event.consumeThreadNums";
    private static final String PROP_EVENT_maxReconsumeTimes = "event.maxReconsumeTimes";
    // 消费者的消息过滤类型, TAG /  SQL92
    private static final String PROP_EVENT_consumerFilterType = "event.consumerFilterType";
    // 消费者的消息过滤表达式 SQL92
    private static final String PROP_EVENT_consumerFilterExpression = "event.consumerFilterExpression";
    private String producerGroup;
    private String consumerGroup;

    private final String channelName;
    private final String server;

    private final String accessKey;
    private final String secretKey;

    private final long timeout;

    //实例的消费线程数，0表示默认
    private final int consumeThreadNums;

    //设置消息消费失败的最大重试次数，0表示默认
    private final int maxReconsumeTimes;

    private final CloudProps cloudProps;
    // 消费者的消息过滤类型, TAG /  SQL92
    private String consumeFilterType;
    // 消费者的消息过滤表达式 SQL92
    private final String consumeFilterExpression;

    public RocketmqConfig(CloudProps cloudProps) {
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

        consumeFilterType = cloudProps.getValue(PROP_EVENT_consumerFilterType);
        consumeFilterExpression = cloudProps.getValue(PROP_EVENT_consumerFilterExpression);
        if (Utils.isEmpty(producerGroup)) {
            producerGroup = "DEFAULT";
        }

        if (Utils.isEmpty(consumerGroup)) {
            consumerGroup = Solon.cfg().appGroup() + "_" + Solon.cfg().appName();
        }
        if (Utils.isEmpty(consumeFilterType)) {
            consumeFilterType = "TAG";
        }

        if (Utils.isEmpty(consumeFilterExpression) && "SQL92".equals(consumeFilterType)) {
            throw new IllegalArgumentException("SQL92 filter expression is empty(event.consumerFilterExpression)");
        }


        log.trace("producerGroup=" + producerGroup);
        log.trace("consumerGroup=" + consumerGroup);
    }

    public CloudProps getCloudProps() {
        return cloudProps;
    }

    /**
     * 消费组
     */
    public String getConsumerGroup() {
        return consumerGroup;
    }

    public String getConsumeFilterType() {
        return consumeFilterType;
    }

    public String getConsumeFilterExpression() {
        return consumeFilterExpression;
    }

    /**
     * 产品组
     */
    public String getProducerGroup() {
        return producerGroup;
    }

    /**
     * 实例的消费线程数，0表示默认
     *
     */
    public int getConsumeThreadNums() {
        return consumeThreadNums;
    }

    /**
     * 设置消息消费失败的最大重试次数，0表示默认
     *
     */
    public int getMaxReconsumeTimes() {
        return maxReconsumeTimes;
    }


    public String getChannelName() {
        return channelName;
    }

    public String getServer() {
        return server;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public long getTimeout() {
        return timeout;
    }
}
