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
package org.noear.solon.cloud.extend.activemq.impl;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ScheduledMessage;
import org.noear.snack.ONode;
import org.noear.solon.cloud.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * @author liuxuehua12
 * @since 2.0
 */
public class ActivemqProducer {
    static Logger log = LoggerFactory.getLogger(ActivemqProducer.class);
    private ActiveMQConnectionFactory factory;
    private Connection connection;

    public ActivemqProducer(ActiveMQConnectionFactory factory) {
        this.factory = factory;
    }

    private void init() throws JMSException {
        if (connection == null) {
            synchronized (factory) {
                if (connection == null) {
                    connection = factory.createConnection();
                    connection.start();
                }
            }
        }
    }

    private void close() throws JMSException {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * 发布事件
     */
    public boolean publish(Event event, String topic) throws Exception {
        long delay = 0;
        if (event.scheduled() != null) {
            delay = event.scheduled().getTime() - System.currentTimeMillis();
        }

        if (delay > 0) {
            return publish(event, topic, delay);
        } else {
            return publish(event, topic, 0);
        }
    }

    public Session beginTransaction() throws JMSException {
        init();

        return connection.createSession(true, Session.CLIENT_ACKNOWLEDGE);
    }

    public boolean publish(Event event, String topic, long delay) throws JMSException {
        init();

        //创建会话
        Session session = null;

        if (event.tran() == null) {
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        } else {
            session = event.tran().getListener(ActivemqTransactionListener.class).getTransaction();
        }

        //创建一个目标
        Destination destination = session.createTopic(topic);
        //创建一个生产者
        MessageProducer producer = session.createProducer(destination);

        //创建消息
        TextMessage message = session.createTextMessage(ONode.stringify(event));

        //支持延时消息
        if (delay > 0) {
            message.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, delay);
        }

        //发布消息
        try {
            producer.send(destination, message);
            return true;
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}
