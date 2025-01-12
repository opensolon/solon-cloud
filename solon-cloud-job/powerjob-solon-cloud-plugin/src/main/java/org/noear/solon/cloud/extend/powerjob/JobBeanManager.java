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
package org.noear.solon.cloud.extend.powerjob;

import org.noear.solon.core.BeanWrap;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于 name 任务包装类管理
 *
 * @author noear
 * @since 2.0
 */
public class JobBeanManager {
    static Map<String, BeanWrap> jobMap = new HashMap<>();

    public static boolean containsJob(String name) {
        return jobMap.containsKey(name);
    }

    public static void addJob(String name, BeanWrap beanWrap) {
        jobMap.put(name, beanWrap);
    }

    public static BeanWrap getJob(String name) {
        return jobMap.get(name);
    }
}
