package features.gateway.funs.exPredicate;

import features.gateway.funs.ExContextEmpty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.exchange.ExPredicate;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.test.SolonTest;

/**
 * 对请求查询参数的断言测试
 *
 * @author noear
 * @since 2.9
 */
@SolonTest
public class QueryPredicateTest {
    RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

    @Test
    public void testEmptyConfig() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Query", "");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Query", null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Query", ",\\d+");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Query", ",");
        });
    }

    @Test
    public void testMatchesHeader() {
        ExPredicate header = routeFactoryManager.getPredicate("Query", "X-Request-Id, \\d+");
        Assertions.assertNotNull(header);

        boolean test = header.test(new ExContextEmpty() {
            @Override
            public String rawQueryParam(String key) {
                return "666";
            }
        });
        Assertions.assertTrue(test);
    }


    @Test
    public void testNotMatchesHeader() {
        ExPredicate header = routeFactoryManager.getPredicate("Query", "X-Request-Id, \\d+");
        Assertions.assertNotNull(header);

        boolean test = header.test(new ExContextEmpty() {
            @Override
            public String rawQueryParam(String key) {
                return "abcd";
            }
        });
        Assertions.assertFalse(test);
    }
}
