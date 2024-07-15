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
package org.noear.solon.cloud.extend.rocketmq.impl;

import org.apache.rocketmq.client.apis.*;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.*;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.model.Event;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * @author noear
 * @since 1.3
 */
public class RocketmqProducer implements Closeable {
    RocketmqConfig config;
    ClientServiceProvider serviceProvider;
    Producer producer;

    public RocketmqProducer(RocketmqConfig config) {
        this.config = config;
    }

    private void lazyInit() throws ClientException {
        if (producer != null) {
            return;
        }

        Utils.locker().lock();

        try {
            if (producer != null) {
                return;
            }


            serviceProvider = ClientServiceProvider.loadService();

            ClientConfigurationBuilder builder = ClientConfiguration.newBuilder();


            //服务地址
            builder.setEndpoints(config.getServer());

            //账号密码
            if (Utils.isNotEmpty(config.getAccessKey())) {
                builder.setCredentialProvider(new StaticSessionCredentialsProvider(config.getAccessKey(), config.getSecretKey()));
            }
            //发送超时时间，默认3000 单位ms
            if (config.getTimeout() > 0) {
                builder.setRequestTimeout(Duration.ofMillis(config.getTimeout()));
            }

            ClientConfiguration configuration = builder.build();

            ProducerBuilder producerBuilder = serviceProvider.newProducerBuilder()
                    .setClientConfiguration(configuration);

            TransactionChecker transactionChecker = Solon.context().getBean(TransactionChecker.class);
            if(transactionChecker == null){
                transactionChecker = new RocketmqTransactionCheckerDefault();
            }
            producerBuilder.setTransactionChecker(transactionChecker);

            producer = producerBuilder.build();

        } finally {
            Utils.locker().unlock();
        }
    }

    public Transaction beginTransaction() throws ClientException {
        lazyInit();

        return producer.beginTransaction();
    }

    public boolean publish(Event event, String topic) throws ClientException {
        lazyInit();

        //普通消息发送。
        Message message = MessageUtil.buildNewMeaage(serviceProvider, event, topic);

        //发送消息，需要关注发送结果，并捕获失败等异常。
        SendReceipt sendReceipt = null;

        if (event.tran() == null) {
            sendReceipt = producer.send(message);
        } else {
            Transaction transaction = event.tran().getListener(RocketmqTransactionListener.class).getTransaction();
            sendReceipt = producer.send(message, transaction);
        }

        if (sendReceipt != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        if (producer != null) {
            producer.close();
        }
    }
}