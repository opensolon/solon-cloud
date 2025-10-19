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
package org.noear.solon.cloud.extend.jmdns.service;

import org.noear.snack4.ONode;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudDiscoveryHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.model.Discovery;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudDiscoveryObserverEntity;
import org.noear.solon.cloud.service.CloudDiscoveryService;
import org.noear.solon.cloud.utils.IntervalUtils;

import javax.jmdns.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author noear 2022/9/14 created
 */
public class CloudDiscoveryServiceJmdnsImpl implements CloudDiscoveryService {
    /**
     * solon 作为固定前缀，区分其他使用 JmDNS 应用
     */
    public static final String PREFIX = "solon";

    /**
     * 服务所属域，使用 local 表示通过 mDNS 在局域网广播从而实现服务发现，其他值则涉及全局DNS服务器
     */
    public static final String DOMAIN = "local";

    /**
     * JmDNS 实例
     */
    JmDNS jmDNS;

    /**
     * 寻找服务超时时间，单位 ms
     * <p>偶尔会因为网络无法正常找到服务，需要重试</p>
     */
    private static final long RETRY_LIMIT_TIME = 5000;

    public CloudDiscoveryServiceJmdnsImpl(CloudProps cloudProps) {
        try {
            String server = cloudProps.getServer().split(":")[0];
            InetAddress jmDNSAddress = "localhost".equals(server) ?
                    InetAddress.getLocalHost() : InetAddress.getByName(server);

            jmDNS = JmDNS.create(jmDNSAddress,
                    jmDNSAddress.toString(),  // jmdns 实例名称，意义不大
                    IntervalUtils.getInterval(cloudProps.getDiscoveryRefreshInterval("100ms")));  // 由于依靠DNS多播，太长会导致服务难以发现
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void register(String group, Instance instance) {
        if(Solon.cfg().appEnabled() == false) {
            return;
        }

        registerState(group, instance, true);
    }

    @Override
    public void registerState(String group, Instance instance, boolean health) {
        int priority = (int) (10 - instance.weight() % 10); // 只取个位和十分位作为权重和优先级，优先级越小越优先
        int weight = (int) ((instance.weight()*10) % 10);   // 权重越大越优先
        Map<String, String> props = new HashMap<>();
        props.put(instance.address(), ONode.serialize(instance));

        ServiceInfo serviceInfo = ServiceInfo.create(
                getType(group),
                instance.address(),
                instance.service(),
                Integer.parseInt(instance.address().split(":")[1]),
                weight,
                priority,
                false,
                props);

        if (health) {
            try {
                jmDNS.registerService(serviceInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            jmDNS.unregisterService(serviceInfo);
        }
    }

    @Override
    public void deregister(String group, Instance instance) {
        registerState(group, instance, false);
    }

    @Override
    public Discovery find(String group, String service) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        Discovery discovery = new Discovery(group, service);

        ServiceInfo[] serviceInfos = getServiceInfos(group).get(service);
        if (serviceInfos != null) {
            for (ServiceInfo serviceInfo : serviceInfos) {
                // 单个服务器上可有多个 ip:port 提供服务，通常只有一个
                Enumeration<String> nodeKeyList = serviceInfo.getPropertyNames();
                while (nodeKeyList.hasMoreElements()) {
                    Instance instance = ONode.deserialize(serviceInfo.getPropertyString(nodeKeyList.nextElement()),
                            Instance.class);
                    discovery.instanceAdd(instance);
                }
            }
        }

        return discovery;
    }

    @Override
    public Collection<String> findServices(String group) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        return getServiceInfos(group).keySet();
    }

    @Override
    public void attention(String group, String service, CloudDiscoveryHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudDiscoveryObserverEntity entity = new CloudDiscoveryObserverEntity(group, service, observer);
        Consumer<ServiceEvent> handle = event -> {
            Discovery discovery = find(entity.group, service);
            entity.handle(discovery);
        };

        jmDNS.addServiceListener(getType(group), new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                handle.accept(event);
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                handle.accept(event);
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                handle.accept(event);
            }
        });
    }

    /**
     * 关闭
     */
    public void close() throws IOException {
        if (jmDNS != null) {
            jmDNS.close();
        }
    }

    /**
     * 限时重试
     */
    private Map<String, ServiceInfo[]> getServiceInfos(String group) {
        Map<String, ServiceInfo[]> serviceMap = Collections.emptyMap();

        String type = getType(group);
        long begin = System.currentTimeMillis();

        while (Utils.isEmpty(serviceMap) && System.currentTimeMillis() - begin < RETRY_LIMIT_TIME) {
            serviceMap = jmDNS.listBySubtype(type);
        }

        return serviceMap;
    }

    /**
     * 组合成 type
     */
    private String getType(String group) {
        return String.format("_%s._%s.%s.", group, PREFIX, DOMAIN);
    }

}
