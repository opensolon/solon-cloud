package demo.rocketmq5;

import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.MessageListener;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.noear.solon.annotation.Component;

@Component
public class DemoMessageListener implements MessageListener {

    @Override
    public ConsumeResult consume(MessageView messageView) {
        System.out.println(messageView);

        return ConsumeResult.SUCCESS;
    }
}