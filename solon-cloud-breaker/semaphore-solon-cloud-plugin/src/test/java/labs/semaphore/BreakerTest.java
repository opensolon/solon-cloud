package labs.semaphore;

import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cloud.annotation.CloudBreaker;
import org.noear.solon.cloud.fallback.Fallback;
import org.noear.solon.cloud.model.BreakerException;
import org.noear.solon.core.aspect.Invocation;
import org.noear.solon.test.SolonTest;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author noear 2025/9/11 created
 */
@SolonTest
public class BreakerTest implements Fallback<Void> {
    @Inject
    DemoService1 demoService1;

    @Inject
    DemoService2 demoService2;

    @Test
    public void case1() { //不会有异常
        Executor executor = Executors.newFixedThreadPool(10);

        executor.execute(demoService1::test);
        executor.execute(demoService1::test);
        executor.execute(demoService1::test);
    }

    @Test
    public void case2() { //会有异常
        Executor executor = Executors.newFixedThreadPool(10);

        executor.execute(demoService2::test);
        executor.execute(demoService2::test);
        executor.execute(demoService2::test);
    }

    @Override
    public Void fallback(Invocation invocation, BreakerException e) throws Throwable {
        return null;
    }

    @CloudBreaker(name = "demo1", fallback = BreakerTest.class)
    @Component
    public static class DemoService1 {
        public void test() {
            System.out.println("test");
        }
    }

    @CloudBreaker(name = "demo2")
    @Component
    public static class DemoService2 {
        public void test() {
            System.out.println("test");
        }
    }
}