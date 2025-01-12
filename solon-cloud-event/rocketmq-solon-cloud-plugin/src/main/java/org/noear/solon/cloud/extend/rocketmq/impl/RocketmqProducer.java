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

import org.apache.rocketmq.acl.common.AclClientRPCHook;
import org.apache.rocketmq.acl.common.SessionCredentials;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;

import java.io.Closeable;
import java.io.IOException;
import java.util.Properties;

/**
 * @author noear
 * @since 1.3
 */
public class RocketmqProducer implements Closeable {
    private final RocketmqConfig config;
    private DefaultMQProducer producer;

    public RocketmqProducer(RocketmqConfig config) {
        this.config = config;
    }

    private void lazyInit() throws MQClientException {
        if (producer != null) {
            return;
        }

        Utils.locker().lock();

        try {
            if (producer != null) {
                return;
            }

            if (Utils.isEmpty(config.getAccessKey())) {
                producer = new DefaultMQProducer();
            } else {
                RPCHook rpcHook = new AclClientRPCHook(new SessionCredentials(config.getAccessKey(), config.getSecretKey()));
                producer = new DefaultMQProducer(rpcHook);
            }


            //服务地址
            producer.setNamesrvAddr(config.getServer());
            //生产组
            producer.setProducerGroup(config.getProducerGroup());
            //命名空间
            if (Utils.isNotEmpty(config.getNamespace())) {
                producer.setNamespace(config.getNamespace());
            }

            //发送超时时间，默认3000 单位ms
            if (config.getTimeout() > 0) {
                producer.setSendMsgTimeout((int) config.getTimeout());
            }
            //失败后重试2次
            producer.setRetryTimesWhenSendFailed(2);

            //绑定定制属性
            Properties props = config.getCloudProps().getEventProducerProps();
            if (props.size() > 0) {
                Utils.injectProperties(producer, props);
            }

            producer.start();
        } finally {
            Utils.locker().unlock();
        }
    }

    public boolean publish(Event event, String topic) throws MQClientException, RemotingException, MQBrokerException, InterruptedException {
        lazyInit();

        Message message = MessageUtil.buildNewMeaage(event, topic);

        SendResult send = producer.send(message);

        if (send.getSendStatus().equals(SendStatus.SEND_OK)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        if (producer != null) {
            producer.shutdown();
        }
    }
}