package demo.activemq;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.util.RunUtil;

import javax.jms.Message;
import javax.jms.MessageListener;

@Component
public class DemoMessageListener implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println(message);
        RunUtil.runAndTry(message::acknowledge);
    }
}