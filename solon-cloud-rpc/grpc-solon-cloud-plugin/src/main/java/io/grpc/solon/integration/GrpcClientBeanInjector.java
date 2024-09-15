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
package io.grpc.solon.integration;

import io.grpc.Channel;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;
import org.noear.solon.core.BeanInjector;
import org.noear.solon.core.VarHolder;
import io.grpc.solon.annotation.GrpcClient;
import org.noear.solon.core.util.ClassUtil;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author noear
 * @since 1.9
 */
public class GrpcClientBeanInjector implements BeanInjector<GrpcClient> {
    private Map<Class<?>, Object> clientMap;

    public GrpcClientBeanInjector(Map<Class<?>, Object> clientMap) {
        this.clientMap = clientMap;
    }

    @Override
    public void doInject(VarHolder vh, GrpcClient anno) {
        vh.required(true);

        Method method;
        Object grpcCli = clientMap.get(vh.getType());

        if (grpcCli != null) {
            vh.setValue(grpcCli);
        } else {
            Channel grpcChannel = new GrpcChannelProxy(anno);
            Class<?> grpcClz = ClassUtil.loadClass(vh.getType().getName().split("\\$")[0]);

            try {
                //同步
                if (AbstractBlockingStub.class.isAssignableFrom(vh.getType())) {
                    method = grpcClz.getDeclaredMethod("newBlockingStub", Channel.class);
                    grpcCli = method.invoke(null, new Object[]{grpcChannel});
                }

                //异步
                if (AbstractFutureStub.class.isAssignableFrom(vh.getType())) {
                    method = grpcClz.getDeclaredMethod("newFutureStub", Channel.class);
                    grpcCli = method.invoke(null, new Object[]{grpcChannel});
                }

                if (grpcCli != null) {
                    clientMap.put(vh.getType(), grpcCli);
                    vh.setValue(grpcCli);
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
