package demo.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

@Controller
public class DemoController {
    @Inject
    private KafkaProducer<String, String> producer;

    @Mapping("/send")
    public void send(String msg) {
        //发送
        producer.send(new ProducerRecord<>("topic.test", msg));
    }
}