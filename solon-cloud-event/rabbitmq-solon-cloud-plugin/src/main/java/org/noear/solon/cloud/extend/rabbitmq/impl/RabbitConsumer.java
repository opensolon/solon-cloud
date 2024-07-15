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
package org.noear.solon.cloud.extend.rabbitmq.impl;

import com.rabbitmq.client.Channel;
import org.noear.solon.cloud.service.CloudEventObserverManger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 消费者
 *
 * @author noear
 * @since 1.3
 * @since 2.4
 */
public class RabbitConsumer {
    private RabbitConfig config;
    private Channel channel;

    private RabbitConsumeHandler handler;
    private RabbitProducer producer;

    public RabbitConsumer(RabbitConfig config, Channel channel, RabbitProducer producer) {
        this.config = config;
        this.channel = channel;
        this.producer = producer;
    }

    /**
     * 初始化
     */
    public void init(CloudEventObserverManger observerManger) throws IOException, TimeoutException {
        handler = new RabbitConsumeHandler(producer, config, channel, observerManger);

        //2.申明队列
        queueDeclareNormal(observerManger);
        queueDeclareReady();
        queueDeclareRetry();
    }


    /**
     * 申明常规队列
     */
    private void queueDeclareNormal(CloudEventObserverManger observerManger) throws IOException {
        Map<String, Object> args = new HashMap<>();

        channel.queueDeclare(config.queue_normal, config.durable, config.exclusive, config.autoDelete, args);

        for (String topic : observerManger.topicAll()) {
            channel.queueBind(config.queue_normal, config.exchangeName, topic, args);
        }

        channel.basicConsume(config.queue_normal, false, handler);
    }

    /**
     * 申明定时队列（即死信队列）
     */
    private void queueDeclareReady() throws IOException {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", config.exchangeName);
        args.put("x-dead-letter-routing-key", config.queue_retry);

        channel.queueDeclare(config.queue_ready, config.durable, config.exclusive, config.autoDelete, args);
        channel.queueBind(config.queue_ready, config.exchangeName, config.queue_ready, args);
    }


    /**
     * 申明重试队列（由定时队列自动转入）
     */
    private void queueDeclareRetry() throws IOException {
        Map<String, Object> args = new HashMap<>();

        channel.queueDeclare(config.queue_retry, config.durable, config.exclusive, config.autoDelete, args);
        channel.queueBind(config.queue_retry, config.exchangeName, config.queue_retry, args);

        channel.basicConsume(config.queue_retry, handler);
    }
}
