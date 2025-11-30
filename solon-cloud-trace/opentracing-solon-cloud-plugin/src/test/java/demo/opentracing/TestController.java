package demo.opentracing;

// --  可以当它不存在得用
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
