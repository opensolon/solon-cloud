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
package org.noear.solon.cloud.extend.aliyun.ons.impl;

import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.ONSFactory;
import com.aliyun.openservices.ons.api.Producer;
import com.aliyun.openservices.ons.api.SendResult;
import org.noear.solon.Utils;
import org.noear.solon.cloud.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author cgy
 * @since 1.11
 */
public class OnsProducer implements Closeable {
    static Logger log = LoggerFactory.getLogger(OnsProducer.class);

    private OnsConfig config;
    private Producer producer;

    public OnsProducer(OnsConfig config) {
        this.config = config;
    }

    private void init() {
        if (producer != null) {
            return;
        }

        Utils.locker().lock();

        try {
            if (producer != null) {
                return;
            }
            producer = ONSFactory.createProducer(config.getProducerProperties());
            producer.start();

            log.debug("Ons producer started: " + producer.isStarted());

        } finally {
            Utils.locker().unlock();
        }
    }

    public boolean publish(Event event, String topic) {
        init();
        //普通消息发送。
        Message message = MessageUtil.buildNewMessage(event, topic);
        //发送消息，需要关注发送结果，并捕获失败等异常。
        SendResult sendReceipt = producer.send(message);
        if (sendReceipt != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void close() throws IOException {

    }
}
