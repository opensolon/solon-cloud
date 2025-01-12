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
package org.noear.solon.cloud.extend.powerjob.impl;

import org.noear.solon.Utils;
import org.noear.solon.cloud.annotation.CloudJob;
import org.noear.solon.cloud.extend.powerjob.JobBeanManager;
import org.noear.solon.core.BeanBuilder;
import org.noear.solon.core.BeanWrap;

/**
 * @author noear
 * @since 2.0
 */
public class PowerJobBeanBuilder implements BeanBuilder<CloudJob> {
    @Override
    public void doBuild(Class<?> clz, BeanWrap bw, CloudJob anno) throws Throwable {
        JobBeanManager.addJob(clz.getName(), bw);

        String name = Utils.annoAlias(anno.value(), anno.name());

        if (Utils.isNotEmpty(name)) {
            JobBeanManager.addJob(name, bw);
        }
    }
}
