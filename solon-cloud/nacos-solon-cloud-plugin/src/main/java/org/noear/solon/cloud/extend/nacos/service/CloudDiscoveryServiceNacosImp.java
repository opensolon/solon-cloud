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
package org.noear.solon.cloud.extend.nacos.service;

import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import org.apache.http.util.TextUtils;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudDiscoveryHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.nacos.impl.InstanceWrap;
import org.noear.solon.cloud.extend.nacos.impl.NacosConfig;
import org.noear.solon.cloud.model.Discovery;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudDiscoveryObserverEntity;
import org.noear.solon.cloud.service.CloudDiscoveryService;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * @author noear
 * @since 1.2
 */
public class CloudDiscoveryServiceNacosImp implements CloudDiscoveryService {
    private NamingService real;

    private boolean unstable;
    private String clusterName;

    public CloudDiscoveryServiceNacosImp(CloudProps cloudProps) {
        Properties properties = NacosConfig.getServiceProperties(cloudProps,
                cloudProps.getProp("discovery"),
                cloudProps.getDiscoveryServer());

        unstable = true;
//        unstable = Solon.cfg().isDriftMode() ||
//                Solon.cfg().isDebugMode();

        clusterName = cloudProps.getDiscoveryClusterName();

        if (Utils.isEmpty(clusterName)) {
            clusterName = "DEFAULT";
        }

        try {
            real = NamingFactory.createNamingService(properties);
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 注册服务实例
     */
    @Override
    public void register(String group, Instance instance) {
        registerState(group, instance, true);
    }

    @Override
    public void registerState(String group, Instance instance, boolean health) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        String[] ss = instance.address().split(":");

        if (ss.length != 2) {
            throw new IllegalArgumentException("Instance.address error");
        }

        InstanceWrap iw = new InstanceWrap();
        iw.setIp(ss[0]);
        iw.setPort(Integer.parseInt(ss[1]));
        iw.setClusterName(clusterName);
        iw.setMetadata(instance.meta());
        iw.setHealthy(health);
        iw.setEphemeral(unstable);
        iw.setWeight(1.0D);
        iw.setEnabled(Solon.cfg().appEnabled());

        try {
            if (Utils.isEmpty(group)) {
                real.registerInstance(instance.service(), iw);
            } else {
                real.registerInstance(instance.service(), group, iw);
            }
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 注销服务实例
     */
    @Override
    public void deregister(String group, Instance instance) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        String[] ss = instance.address().split(":");

        if (ss.length != 2) {
            throw new IllegalArgumentException("Instance.address error");
        }

        InstanceWrap iw = new InstanceWrap();
        iw.setIp(ss[0]);
        iw.setPort(Integer.parseInt(ss[1]));
        iw.setClusterName(clusterName);
        iw.setMetadata(instance.meta());
        iw.setHealthy(false);
        iw.setEphemeral(unstable);
        iw.setWeight(1.0D);

        try {
            if (Utils.isEmpty(group)) {
                real.deregisterInstance(instance.service(), iw);
            } else {
                real.deregisterInstance(instance.service(), group, iw);
            }
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 查询服务实例列表
     */
    @Override
    public Discovery find(String group, String service) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        Discovery discovery = new Discovery(group, service);

        try {
            List<com.alibaba.nacos.api.naming.pojo.Instance> list = null;

            if (Utils.isEmpty(group)) {
                list = real.selectInstances(service, true);
            } else {
                list = real.selectInstances(service, group, true);
            }

            for (com.alibaba.nacos.api.naming.pojo.Instance i1 : list) {
                Instance n1 = new Instance(service, i1.getIp(), i1.getPort())
                        .weight(i1.getWeight())
                        .metaPutAll(i1.getMetadata()); //会自动处理 protocol

                //添加集群名
                n1.metaPut(PropertyKeyConst.CLUSTER_NAME, i1.getClusterName());

                discovery.instanceAdd(n1);
            }

            return discovery;
        } catch (NacosException ex) {
            throw new RuntimeException();
        }
    }

    @Override
    public Collection<String> findServices(String group) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        try {
            return real.getServicesOfServer(1, Integer.MAX_VALUE, group)
                    .getData();
        } catch (NacosException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 关注服务实例列表
     */
    @Override
    public void attention(String group, String service, CloudDiscoveryHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudDiscoveryObserverEntity entity = new CloudDiscoveryObserverEntity(group, service, observer);

        try {
            if (TextUtils.isEmpty(group)) {
                real.subscribe(service, (event) -> {
                    Discovery discovery = find(entity.group, service);
                    entity.handle(discovery);
                });

            } else {
                real.subscribe(service, group, (event) -> {
                    Discovery discovery = find(entity.group, service);
                    entity.handle(discovery);
                });

            }
        } catch (NacosException ex) {
            throw new RuntimeException();
        }
    }
}