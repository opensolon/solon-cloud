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
package org.noear.solon.cloud.extend.xxljob.integration;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import org.noear.solon.core.BeanExtractor;
import org.noear.solon.core.BeanWrap;

import java.lang.reflect.Method;

/**
 * @author noear
 * @since 1.4
 */
class XxlJobExtractor implements BeanExtractor<XxlJob> {
    @Override
    public void doExtract(BeanWrap bw, Method method, XxlJob anno) {
        String name = anno.value();

        if (name.trim().length() == 0) {
            throw new IllegalStateException("xxl-job method-jobhandler name invalid, for[" + bw.clz() + "#" + method.getName() + "] .");
        }
        if (XxlJobExecutor.loadJobHandler(name) != null) {
            throw new IllegalStateException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }


        method.setAccessible(true);

        // init and destroy
        Method initMethod = null;
        Method destroyMethod = null;

        if (anno.init().trim().length() > 0) {
            try {
                initMethod = bw.clz().getDeclaredMethod(anno.init());
                initMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("xxl-job method-jobhandler initMethod invalid, for[" + bw.clz() + "#" + method.getName() + "] .");
            }
        }
        if (anno.destroy().trim().length() > 0) {
            try {
                destroyMethod = bw.clz().getDeclaredMethod(anno.destroy());
                destroyMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("xxl-job method-jobhandler destroyMethod invalid, for[" + bw.clz() + "#" + method.getName() + "] .");
            }
        }

        //提示：不支持CloudJob拦截器
        XxlJobExecutor.registJobHandler(name, new MethodJobHandler(bw.raw(), method, initMethod, destroyMethod));
    }
}
