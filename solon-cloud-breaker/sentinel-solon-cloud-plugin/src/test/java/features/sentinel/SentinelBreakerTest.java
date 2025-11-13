package features.sentinel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cloud.annotation.CloudBreaker;
import org.noear.solon.cloud.fallback.Fallback;
import org.noear.solon.cloud.model.BreakerException;
import org.noear.solon.core.aspect.Invocation;
import org.noear.solon.test.SolonTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author noear 2025/9/11 created
 */
@SolonTest
public class SentinelBreakerTest implements Fallback<Void> {
    @Inject
    DemoService1 demoService1;

    @Inject
    DemoService2 demoService2;

    @Test
    public void case1() throws Throwable { //不会有异常
        Executor executor = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                try {
                    demoService1.test();
                    count.incrementAndGet();
                } catch (Throwable e) {
                    error.set(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        System.out.println(count.get());

        Assertions.assertNull(error.get());
        Assertions.assertEquals(10, count.get());
        Assertions.assertEquals(1, demoService1.getCount().get());

    }

    @Test
    public void case2() throws Throwable { //会有异常
        Executor executor = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicInteger count = new AtomicInteger(0);

        for (int i = 0; i < 10; i++) {
            executor.execute(() -> {
                try {
                    demoService2.test();
                    count.incrementAndGet();
                } catch (Throwable e) {
                    error.set(e);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        System.out.println(count.get());

        Assertions.assertNotNull(error.get());
        Assertions.assertEquals(1, count.get());
        Assertions.assertEquals(1, demoService2.getCount().get());
    }

    @Override
    public Void fallback(Invocation invocation, BreakerException e) throws Throwable {
        return null;
    }

    @Component
    public static class DemoService1 {
        private AtomicInteger count = new AtomicInteger(0);

        public AtomicInteger getCount() {
            return count;
        }

        @CloudBreaker(name = "demo1", fallback = SentinelBreakerTest.class)
        public void test() {
            count.incrementAndGet();
            System.out.println("test");
        }
    }

    @Component
    public static class DemoService2 {
        private AtomicInteger count = new AtomicInteger(0);

        public AtomicInteger getCount() {
            return count;
        }

        @CloudBreaker(name = "demo2")
        public void test() {
            count.incrementAndGet();
            System.out.println("test2");
        }
    }
}