package features.gateway.funs.router;

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.cloud.gateway.route.handler.HttpRouteHandler;
import org.noear.solon.cloud.gateway.route.handler.LbRouteHandler;

import java.net.URI;

/**
 *
 * @author noear 2026/5/11 created
 *
 */
public class RouterTest {
    @Test
    public void http() {
        RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

        URI uri = URI.create("http://xxx");
        RouteHandler routeHandler = routeFactoryManager.getHandler(uri.getScheme());

        assert routeHandler != null;
        assert routeHandler instanceof HttpRouteHandler;
    }

    @Test
    public void https() {
        RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

        URI uri = URI.create("https://xxx");
        RouteHandler routeHandler = routeFactoryManager.getHandler(uri.getScheme());

        assert routeHandler != null;
        assert routeHandler instanceof HttpRouteHandler;
    }

    @Test
    public void lb() {
        RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

        URI uri = URI.create("lb://a.b.c/d?e=1");
        System.out.println(uri.getScheme());
        RouteHandler routeHandler = routeFactoryManager.getHandler(uri.getScheme());

        assert routeHandler != null;
        assert routeHandler instanceof LbRouteHandler;
        assert uri.getSchemeSpecificPart().contains("://") == false;
    }

    @Test
    public void lb_ws() {
        RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

        URI uri = URI.create("lb:ws://a.b.c/d?e=1");
        System.out.println(uri.getScheme());
        RouteHandler routeHandler = routeFactoryManager.getHandler(uri.getScheme());

        assert routeHandler != null;
        assert routeHandler instanceof LbRouteHandler;
        assert uri.getSchemeSpecificPart().contains("://") == true;
    }
}
