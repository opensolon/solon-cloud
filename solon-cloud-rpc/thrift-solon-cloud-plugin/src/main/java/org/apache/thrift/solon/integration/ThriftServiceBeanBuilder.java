/*
 * Copyright 2017-2024 noear.org and authors
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
package org.apache.thrift.solon.integration;

import org.apache.thrift.solon.annotation.ThriftService;
import org.noear.solon.core.BeanBuilder;
import org.noear.solon.core.BeanWrap;

import java.util.Map;

/**
 * 针对添加了 @ThriftService 的类进行构建
 *
 * @author LIAO.Chunping
 */
public class ThriftServiceBeanBuilder implements BeanBuilder<ThriftService> {

    private Map<Class<?>, Object> serviceMap;

    public ThriftServiceBeanBuilder(Map<Class<?>, Object> serviceMap) {
        this.serviceMap = serviceMap;
    }

    @Override
    public void doBuild(Class<?> clz, BeanWrap bw, ThriftService anno) throws Throwable {
        serviceMap.put(clz, bw.raw());
    }
}
