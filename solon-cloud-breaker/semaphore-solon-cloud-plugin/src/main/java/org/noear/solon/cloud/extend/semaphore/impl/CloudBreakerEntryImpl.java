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
package org.noear.solon.cloud.extend.semaphore.impl;

import org.noear.solon.cloud.model.BreakerEntrySim;
import org.noear.solon.cloud.model.BreakerException;

import java.util.concurrent.Semaphore;

/**
 * @author noear
 * @since 1.3
 */
public class CloudBreakerEntryImpl extends BreakerEntrySim {
    private String breakerName;
    private int thresholdValue;
    private Semaphore limiter;

    public CloudBreakerEntryImpl(String breakerName, int permits) {
        this.breakerName = breakerName;
        this.thresholdValue = permits;
        this.limiter = new Semaphore(permits);
    }

    @Override
    public AutoCloseable enter() throws BreakerException {
        if (limiter.tryAcquire()) {
            return this;
        } else {
            throw new BreakerException();
        }
    }

    @Override
    public void close() throws Exception {
        limiter.release();
    }

    @Override
    public void reset(int value) {
        if (thresholdValue != value) {
            thresholdValue = value;

            limiter = new Semaphore(value);
        }
    }
}
