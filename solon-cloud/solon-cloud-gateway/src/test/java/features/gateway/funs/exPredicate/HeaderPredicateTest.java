package features.gateway.funs.exPredicate;

import features.gateway.funs.ExContextEmpty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.exchange.ExPredicate;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.test.SolonTest;

/**
 * 对请求头的断言测试
 *
 * @author wjc28
 * @since 2.9
 */
@SolonTest
public class HeaderPredicateTest {

    RouteFactoryManager routeFactoryManager = new RouteFactoryManager();

    @Test
    public void testEmptyConfig() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Header", "");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Header", null);
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Header", ",\\d+");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            routeFactoryManager.getPredicate("Header", ",");
        });
    }

    @Test
    public void testMatchesHeader() {
        ExPredicate header = routeFactoryManager.getPredicate("Header", "X-Request-Id, \\d+");
        Assertions.assertNotNull(header);

        boolean test = header.test(new ExContextEmpty() {
            @Override
            public String rawHeader(String key) {
                return "666";
            }
        });
        Assertions.assertTrue(test);
    }

    @Test
    public void testMatchesHeader2() {
        ExPredicate header = routeFactoryManager.getPredicate("Header", "X-Request-Id, abc");
        Assertions.assertNotNull(header);

        boolean test = header.test(new ExContextEmpty() {
            @Override
            public String rawHeader(String key) {
                return "abc";
            }
        });
        Assertions.assertTrue(test);
    }


    @Test
    public void testNotMatchesHeader() {
        ExPredicate header = routeFactoryManager.getPredicate("Header", "X-Request-Id, \\d+");
        Assertions.assertNotNull(header);

        boolean test = header.test(new ExContextEmpty() {
            @Override
            public String rawHeader(String key) {
                return "abcd";
            }
        });
        Assertions.assertFalse(test);
    }
}
