package demo.activemq;

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

@Controller
public class DemoController {
    @Inject
    private IProducer producer;

    @Mapping("/send")
    public void send(String msg) throws Exception {
        //发送
        producer.send("topic.test", s -> s.createTextMessage("test"));
    }
}