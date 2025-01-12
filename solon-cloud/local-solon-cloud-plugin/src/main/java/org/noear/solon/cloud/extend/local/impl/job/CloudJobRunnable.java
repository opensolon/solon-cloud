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
package org.noear.solon.cloud.extend.local.impl.job;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudJobHandler;
import org.noear.solon.cloud.exception.CloudJobException;
import org.noear.solon.core.handle.ContextEmpty;

/**
 * 方法运行器
 *
 * @author noear
 * @since 1.6
 */
public class CloudJobRunnable implements Runnable {
    private CloudJobHandler handler;

    public CloudJobRunnable(CloudJobHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            handler.handle(new ContextEmpty());
        } catch (Throwable e) {
            e = Utils.throwableUnwrap(e);
            throw new CloudJobException(e);
        }
    }
}
