package demo.opentelemetry;

import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.telemetry.Spans;
import org.noear.solon.cloud.telemetry.annotation.Tracing;

//-- 通过注解增加业务链节点 ( @Tracing )
@Component
public class OrderService {
    @Tracing(name = "创建订单", tags = "订单=#{orderId}")
    public String orderCreate(String userName, String orderId) {
        //手动添加 tag
        Spans.active().setAttribute("用户", userName);

        return orderId;
    }
}