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
package org.noear.solon.cloud.extend.rabbitmq.impl;

import com.rabbitmq.client.*;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.core.util.RunUtil;

import java.io.IOException;

/**
 * 消费者接收处理
 *
 * @author noear
 * @since 1.3
 * @since 2.6
 */
public class RabbitConsumeHandler extends DefaultConsumer {

    protected final CloudEventObserverManger observerManger;
    protected final RabbitConfig config;
    protected final RabbitProducer producer;
    protected final String eventChannelName;

    public RabbitConsumeHandler(RabbitProducer producer, RabbitConfig config, Channel channel, CloudEventObserverManger observerManger) {
        super(channel);
        this.config = config;
        this.producer = producer;
        this.observerManger = observerManger;
        this.eventChannelName = config.getEventChannel();
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        RunUtil.async(new RabbitConsumeTask(this, consumerTag, envelope, properties, body));
    }
}