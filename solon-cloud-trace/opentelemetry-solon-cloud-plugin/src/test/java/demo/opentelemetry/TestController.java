package demo.opentelemetry;

import org.noear.nami.annotation.NamiClient;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;

@Controller
public class TestController {
    @NamiClient
    UserService userService;

    @Inject
    OrderService orderService;

    @Mapping("/")
    public String hello(String name) {
        name = userService.getUser(name);

        return orderService.orderCreate(name, "1");
    }
}