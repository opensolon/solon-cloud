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
package org.noear.solon.cloud.extend.kafka.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Header;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.EventLevel;
import org.noear.solon.cloud.exception.CloudEventException;
import org.noear.solon.cloud.extend.kafka.impl.KafkaConfig;
import org.noear.solon.cloud.extend.kafka.impl.KafkaTransactionListener;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.cloud.model.EventTran;
import org.noear.solon.cloud.service.CloudEventObserverManger;
import org.noear.solon.cloud.service.CloudEventServicePlus;
import org.noear.solon.cloud.utils.ExpirationUtils;
import org.noear.solon.core.bean.LifecycleBean;
import org.noear.solon.core.util.RunUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author noear
 * @since 1.3
 */
public class CloudEventServiceKafkaImpl implements CloudEventServicePlus, Closeable, LifecycleBean {
    private static final Logger log = LoggerFactory.getLogger(CloudEventServiceKafkaImpl.class);

    private final KafkaConfig config;
    private KafkaProducer<String, String> producer;
    private KafkaProducer<String, String> producerTran;
    private KafkaConsumer<String, String> consumer;
    private Future<?> consumerFuture;

    public CloudEventServiceKafkaImpl(CloudProps cloudProps) {
        this.config = new KafkaConfig(cloudProps);
    }

    private void initProducer() {
        if (producer != null) {
            return;
        }

        Utils.locker().lock();

        try {
            if (producer != null) {
                return;
            }

            producer = new KafkaProducer<>(config.getProducerProperties(false));

            //支持事务
            producerTran = new KafkaProducer<>(config.getProducerProperties(true));
            producerTran.initTransactions();
        } finally {
            Utils.locker().unlock();
        }
    }

    private void initConsumer() {
        if (consumer != null) {
            return;
        }

        Utils.locker().lock();

        try {
            if (consumer != null) {
                return;
            }

            Properties properties = config.getConsumerProperties();
            consumer = new KafkaConsumer<>(properties);
        } finally {
            Utils.locker().unlock();
        }
    }

    private void beginTransaction(EventTran transaction) throws CloudEventException {
        if (transaction.getListener(KafkaTransactionListener.class) != null) {
            return;
        }

        try {
            producerTran.beginTransaction();
            transaction.setListener(new KafkaTransactionListener(producerTran));
        } catch (Exception e) {
            throw new CloudEventException(e);
        }
    }

    @Override
    public boolean publish(Event event) throws CloudEventException {
        initProducer();

        if (Utils.isEmpty(event.key())) {
            event.key(Utils.guid());
        }

        if (event.created() == 0L) {
            event.created(System.currentTimeMillis());
        }

        if (event.tran() != null) {
            beginTransaction(event.tran());
        }

        Future<RecordMetadata> future = null;

        ProducerRecord<String, String> record = new ProducerRecord<>(event.topic(), null, event.created(), event.key(), event.content());

        //@since 3.0
        for (Map.Entry<String, String> kv : event.meta().entrySet()) {
            record.headers().add(kv.getKey(), kv.getValue().getBytes(StandardCharsets.UTF_8));
        }

        if (event.tran() == null) {
            future = producer.send(record);
        } else {
            future = producerTran.send(record);
        }

        if (config.getPublishTimeout() > 0 && event.qos() > 0) {
            try {
                future.get(config.getPublishTimeout(), TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new CloudEventException(e);
            }
        }

        return true;
    }

    private CloudEventObserverManger observerManger = new CloudEventObserverManger();

    @Override
    public void attention(EventLevel level, String channel, String group, String topic, String tag, int qos, CloudEventHandler observer) {
        observerManger.add(topic, level, group, topic, tag, qos, observer);
    }

    @Override
    public void postStart() throws Throwable {
        //订阅开始
        if (observerManger.topicSize() > 0) {

            initConsumer();
            consumer.subscribe(observerManger.topicAll());

            //开始拉取
            consumerFuture = RunUtil.parallel(this::subscribePull);
        }
    }


    //接口超时
    private final long poll_timeout_ms = 1_000; //1s
    //消费失败次数
    private final AtomicInteger consume_failure_times = new AtomicInteger(0);

    private void subscribePull() {
        try {
            if (subscribePull0() == 0) {
                //如果没有数据，隔几秒
                consume_failure_times.set(1);
            }
        } catch (EOFException e) {
            return;
        } catch (Throwable e) {
            if (e instanceof InterruptedException) {
                return;
            }

            consume_failure_times.incrementAndGet();
            log.warn(e.getMessage(), e);
        }

        int times = consume_failure_times.get();

        if (times > 0) {
            if (times > 99) {
                consume_failure_times.set(99);
            }

            consumerFuture = RunUtil.delay(this::subscribePull, ExpirationUtils.getExpiration(times));
        } else {
            consumerFuture = RunUtil.parallel(this::subscribePull);
        }
    }

    private int subscribePull0() throws Throwable {
        //拉取
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(poll_timeout_ms));

        Map<TopicPartition, OffsetAndMetadata> topicOffsets = new LinkedHashMap<>();

        //如果异常，就中止 for；把已收集的 topicOffsets 提交掉；然后重新拉取
        for (ConsumerRecord<String, String> record : records) {
            Event event = new Event(record.topic(), record.value())
                    .key(record.key())
                    .channel(config.getEventChannel());

            //@since 3.1
            event.created(record.timestamp());

            //@since 3.0
            for (Header h1 : record.headers()) {
                event.meta().put(h1.key(), new String(h1.value(), StandardCharsets.UTF_8));
            }

            //接收并处理事件
            TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
            if (onReceive(event)) {
                //接收需要提交的偏移量（如果成果，重置失败次数）
                consume_failure_times.set(0);
                topicOffsets.put(topicPartition, new OffsetAndMetadata(record.offset() + 1));
            } else {
                //如果失败了，从失败的地方重试，避免丢失进度
                log.warn("Event processing failed, retrying from the failed location. topic:{}; partition:{}; offset:{}",
                        record.topic(), record.partition(), record.offset());

                consume_failure_times.incrementAndGet();
                consumer.seek(topicPartition, record.offset());
                break;
            }
        }


        if (topicOffsets.size() > 0) {
            consumer.commitSync(topicOffsets);
        }

        return records.count();
    }

    /**
     * 处理接收事件
     */
    protected boolean onReceive(Event event) {
        try {
            return onReceiveDo(event);
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 处理接收事件
     */
    protected boolean onReceiveDo(Event event) throws Throwable {
        boolean isOk = true;
        CloudEventHandler handler = null;

        handler = observerManger.getByTopic(event.topic());
        if (handler != null) {
            isOk = handler.handle(event);
        } else {
            //只需要记录一下
            log.warn("There is no observer for this event topic[{}]", event.topic());
        }

        return isOk;
    }

    @Override
    public String getChannel() {
        return config.getEventChannel();
    }

    @Override
    public String getGroup() {
        return config.getEventGroup();
    }

    @Override
    public void close() throws IOException {
        if (producer != null) {
            producer.close();
        }

        if (producerTran != null) {
            producerTran.close();
        }

        if (consumer != null) {
            consumer.close();
        }

        if (consumerFuture != null) {
            consumerFuture.cancel(true);
        }
    }
}