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
package org.noear.solon.cloud.extend.polaris.service;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import com.tencent.polaris.factory.config.global.ClusterConfigImpl;
import com.tencent.polaris.factory.config.global.ServerConnectorConfigImpl;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudDiscoveryHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.polaris.PolarisProps;
import org.noear.solon.cloud.model.Discovery;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.CloudDiscoveryObserverEntity;
import org.noear.solon.cloud.service.CloudDiscoveryService;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author 何荣振
 * @since 1.11
 * */
public class CloudDiscoveryServicePolarisImp implements CloudDiscoveryService , Closeable {
    private final CloudProps cloudProps;
    private ProviderAPI providerAPI;
    private ConsumerAPI consumerAPI;

    public CloudDiscoveryServicePolarisImp(CloudProps cloudProps) {
        this.cloudProps = cloudProps;
        String server = cloudProps.getDiscoveryServer();

        ConfigurationImpl cfgImpl = PolarisProps.getCfgImpl();

        //发现集群设置
        ClusterConfigImpl clusterConfig = cfgImpl.getGlobal().getSystem().getDiscoverCluster();
        clusterConfig.setNamespace(cloudProps.getNamespace());

        //发现连接设置(8091)
        ServerConnectorConfigImpl connectorConfig = cfgImpl.getGlobal().getServerConnector();
        List<String> address = connectorConfig.getAddresses();
        address.add(server);
        connectorConfig.setAddresses(address);

        providerAPI = DiscoveryAPIFactory.createProviderAPIByConfig(cfgImpl);
        consumerAPI = DiscoveryAPIFactory.createConsumerAPIByConfig(cfgImpl);
    }

    /**
     * 注册服务实例
     *
     * @param group    分组
     * @param instance 服务实例
     */
    @Override
    public void register(String group, Instance instance) {
        registerState(group, instance, true);
    }

    /**
     * 注册服务实例健康状态
     *
     * @param group    分组
     * @param instance 服务实例
     */
    @Override
    public void registerState(String group, Instance instance, boolean health) {
        String[] ss = instance.address().split(":");

        if (ss.length != 2) {
            throw new IllegalArgumentException("Instance.address error");
        }

        InstanceRegisterRequest request = new InstanceRegisterRequest();
        request.setNamespace(cloudProps.getNamespace());
        request.setWeight((int) instance.weight());
        request.setMetadata(instance.meta());
        request.setService(instance.service());
        request.setHost(ss[0]);
        request.setPort(Integer.parseInt(ss[1]));
        request.setProtocol(instance.protocol());

        providerAPI.registerInstance(request);
    }

    /**
     * 注销服务实例
     *
     * @param group    分组
     * @param instance 服务实例
     */
    @Override
    public void deregister(String group, Instance instance) {
        String[] ss = instance.address().split(":");

        if (ss.length != 2) {
            throw new IllegalArgumentException("Instance.address error");
        }
        InstanceDeregisterRequest deregisterRequest = new InstanceDeregisterRequest();

        deregisterRequest.setNamespace(cloudProps.getNamespace());
        deregisterRequest.setService(instance.service());
        deregisterRequest.setHost(ss[0]);
        deregisterRequest.setPort(Integer.parseInt(ss[1]));

        providerAPI.deRegister(deregisterRequest);
    }

    /**
     * @param group   分组
     * @param service 服各名
     * @return
     */
    @Override
    public Discovery find(String group, String service) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        Discovery discovery = new Discovery(group, service);

        GetHealthyInstancesRequest request = new GetHealthyInstancesRequest();

        request.setNamespace(cloudProps.getNamespace());
        request.setService(service);

        InstancesResponse instancesResponse = consumerAPI.getHealthyInstances(request);

        if (Objects.isNull(instancesResponse) || instancesResponse.getInstances().length > 0) {
            return discovery;
        }

        for (com.tencent.polaris.api.pojo.Instance instance : instancesResponse.getInstances()) {
            //只关注健康的
            if (instance.isHealthy()) {
                discovery.instanceAdd(new Instance(service,
                        instance.getHost() + ":" + instance.getPort())
                        .weight(instance.getWeight())
                        .protocol(instance.getProtocol())
                        .metaPutAll(instance.getMetadata()));
            }
        }

        return discovery;
    }

    /**
     * 关注服务实例列表
     *
     * @param group    分组
     * @param service  服各名
     * @param observer 观察者
     */
    @Override
    public void attention(String group, String service, CloudDiscoveryHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudDiscoveryObserverEntity entity = new CloudDiscoveryObserverEntity(group, service, observer);

        WatchServiceRequest request = WatchServiceRequest.builder()
                .namespace(cloudProps.getNamespace())
                .service(service)
                .listeners(Collections.singletonList(event -> {
                    Discovery discovery = new Discovery(entity.group, service);

                    for (com.tencent.polaris.api.pojo.Instance instance : event.getAllInstances()) {
                        if (instance.isHealthy()) {
                            discovery.instanceAdd(new Instance(service,
                                    instance.getHost() + ":" + instance.getPort())
                                    .weight(instance.getWeight())
                                    .protocol(instance.getProtocol())
                                    .metaPutAll(instance.getMetadata()));
                        }
                    }

                    entity.handle(discovery);
                }))
                .build();

        consumerAPI.watchService(request);
    }

    @Override
    public void close() throws IOException {
        if (consumerAPI != null) {
            consumerAPI.close();
        }

        if (providerAPI != null) {
            providerAPI.close();
        }
    }
}