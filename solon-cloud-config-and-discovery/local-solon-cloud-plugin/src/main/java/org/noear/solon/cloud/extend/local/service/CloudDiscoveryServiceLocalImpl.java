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
package org.noear.solon.cloud.extend.local.service;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudDiscoveryHandler;
import org.noear.solon.cloud.model.Discovery;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudDiscoveryObserverEntity;
import org.noear.solon.cloud.service.CloudDiscoveryService;

import java.util.*;

/**
 * 云端注册与发现（本地摸拟实现）
 *
 * @author noear
 * @since 1.10
 */
public class CloudDiscoveryServiceLocalImpl implements CloudDiscoveryService {
    private Map<String, Discovery> serviceMap = new HashMap<>();
    private List<CloudDiscoveryObserverEntity> observerList = new ArrayList<>();

    @Override
    public void register(String group, Instance instance) {
        if(Solon.cfg().appEnabled() == false) {
            return;
        }

        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        Discovery discovery = serviceMap.get(instance.service());
        if (discovery == null) {
            discovery = new Discovery(group, instance.service());
            serviceMap.put(instance.service(), discovery);
        }

        discovery.instanceAdd(instance);

        //通知更新
        onRegister(discovery);
    }

    @Override
    public void registerState(String group, Instance instance, boolean health) {

    }

    @Override
    public void deregister(String group, Instance instance) {

    }

    @Override
    public Discovery find(String group, String service) {
        Discovery tmp = serviceMap.get(service);
        if (tmp == null) {
            tmp = new Discovery(group, service);
        }

        return tmp;
    }

    @Override
    public Collection<String> findServices(String group) {
        return serviceMap.keySet();
    }

    @Override
    public void attention(String group, String service, CloudDiscoveryHandler observer) {
        observerList.add(new CloudDiscoveryObserverEntity(group, service, observer));
    }

    /**
     * 通知观察者
     */
    private void onRegister(Discovery discovery) {
        if (serviceMap.containsKey(discovery.service())) {

            for (CloudDiscoveryObserverEntity entity : observerList) {
                if (discovery.service().equals(entity.service)) {
                    entity.handle(discovery);
                }
            }
        }
    }
}