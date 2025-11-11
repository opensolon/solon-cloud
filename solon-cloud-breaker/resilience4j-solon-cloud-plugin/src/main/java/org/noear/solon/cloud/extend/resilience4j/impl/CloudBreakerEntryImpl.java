/*
 * Copyright 2017-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.cloud.extend.resilience4j.impl;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.noear.solon.cloud.model.BreakerEntrySim;
import org.noear.solon.cloud.model.BreakerException;

import java.time.Duration;

/**
 * @author stephondng
 * @since 3.7.1
 */
public class CloudBreakerEntryImpl extends BreakerEntrySim {
    private String breakerName;
    private int thresholdValue;
    private RateLimiter rateLimiter;

    public CloudBreakerEntryImpl(String breakerName, int permitsPerSecond) {
        this.breakerName = breakerName;
        this.thresholdValue = permitsPerSecond;

        loadRules();
    }

    private void loadRules() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(thresholdValue)
                .timeoutDuration(Duration.ofSeconds(1))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        rateLimiter = registry.rateLimiter(breakerName, config);
    }

    @Override
    public AutoCloseable enter() throws BreakerException {
        if (rateLimiter.acquirePermission()) {
            return this;
        } else {
            throw new BreakerException();
        }
    }

    @Override
    public void close() {
        // resilience4j 会自动释放资源，这里不需要额外处理
    }

    @Override
    public void reset(int value) {
        if (thresholdValue != value) {
            thresholdValue = value;
            loadRules();
        }
    }
}