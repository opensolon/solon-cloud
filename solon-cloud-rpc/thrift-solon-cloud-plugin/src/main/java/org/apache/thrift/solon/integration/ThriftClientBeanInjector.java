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
package org.apache.thrift.solon.integration;

import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.solon.annotation.ThriftClient;
import org.noear.solon.Solon;
import org.noear.solon.core.BeanInjector;
import org.noear.solon.core.VarHolder;
import org.noear.solon.proxy.asm.AsmProxy;

import java.lang.reflect.Constructor;
import java.util.Map;

/**
 * 添加了 @ThriftClient 注解的字段进行注入对象
 *
 * @author LIAO.Chunping
 */
public class ThriftClientBeanInjector implements BeanInjector<ThriftClient> {

    private Map<Class<?>, Object> clientMap;

    public ThriftClientBeanInjector(Map<Class<?>, Object> clientMap) {
        this.clientMap = clientMap;
    }

    @Override
    public void doInject(VarHolder vh, ThriftClient anno) {
        vh.required(true);

        Object thriftClient = clientMap.get(vh.getType());
        if (thriftClient != null) {
            vh.setValue(thriftClient);
            return;
        }

        try {
            // 找到 Client 中，带 TProtocol.class 的构造器，将其初始化
            Class<?> clientType = vh.getType();
            Constructor<?> declaredConstructor = clientType.getDeclaredConstructor(TProtocol.class);

            // 创建 Client 代理，在代理中将打开/关闭 Socket 连接
            Object clientProxy = AsmProxy.newProxyInstance(vh.context(), new ThriftClientProxy(anno, clientType), clientType, declaredConstructor,  new Object[]{null});
            vh.setValue(clientProxy);
            clientMap.put(clientType, clientProxy);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
