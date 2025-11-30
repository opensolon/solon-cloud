package demo.opentracing;

import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.tracing.Spans;
import org.noear.solon.cloud.tracing.annotation.Tracing;

//-- 通过注解增加业务链节点 ( @Tracing )
@Component
public class OrderService {
    @Tracing(name = "创建订单", tags = "订单=${orderId}")
    public String orderCreate(String userName, String orderId) {
        //手动添加 tag
        Spans.active().setTag("用户", userName);

        return orderId;
    }
}