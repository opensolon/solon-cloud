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
package org.noear.solon.cloud.extend.activemq.impl;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * @author liuxuehua12
 * @since 2.0
 */
public class ActivemqConsumer {
	static Logger log = LoggerFactory.getLogger(ActivemqConsumer.class);
	private ActiveMQConnectionFactory factory;
	private ActivemqProducer producer;
	private Connection connection;
	private ActivemqConsumeHandler handler;

	public ActivemqConsumer(ActiveMQConnectionFactory factory, ActivemqProducer producer) {
		this.factory = factory;
		this.producer = producer;
	}

	public void init(CloudEventObserverManger observerManger) throws JMSException {
		if (connection != null) {
			return;
		}

		connection = factory.createConnection();
		connection.start();
		//创建会话
		Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		handler = new ActivemqConsumeHandler(observerManger, session);

		//订阅所有主题
		for (String topic : observerManger.topicAll()) {
			//创建一个目标
			Destination destination = session.createTopic(topic);
			//创建一个消费者
			MessageConsumer consumer = session.createConsumer(destination);

			consumer.setMessageListener(handler);
		}
	}

	public void close() throws JMSException {
		if (connection != null) {
			connection.close();
		}
	}
}
