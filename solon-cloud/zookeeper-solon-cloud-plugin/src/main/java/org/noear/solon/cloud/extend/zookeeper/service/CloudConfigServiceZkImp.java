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
package org.noear.solon.cloud.extend.zookeeper.service;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudConfigHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.zookeeper.impl.ZkClient;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.service.CloudConfigObserverEntity;
import org.noear.solon.cloud.service.CloudConfigService;

/**
 * @author noear
 * @since 1.3
 */
public class CloudConfigServiceZkImp implements CloudConfigService {
    private static final String PATH_ROOT = "/solon/config";
    private ZkClient client;

    public CloudConfigServiceZkImp(CloudProps cloudProps) {
        //默认3秒
        String sessionTimeout = cloudProps.getDiscoveryHealthCheckInterval("3000");
        this.client = new ZkClient(cloudProps.getDiscoveryServer(), Integer.parseInt(sessionTimeout));


        this.client.connectServer();

        this.client.createNode("/solon");
        this.client.createNode(PATH_ROOT);
    }

    @Override
    public Config pull(String group, String key) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        String value = client.getNodeData(
                String.format("%s/%s/%s", PATH_ROOT, group, key));

        return new Config(group, key, value, 0);
    }

    @Override
    public boolean push(String group, String key, String value) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        client.createNode(
                String.format("%s/%s", PATH_ROOT, group));

        client.setNodeData(
                String.format("%s/%s/%s", PATH_ROOT, group, key),
                value);

        return true;
    }

    @Override
    public boolean remove(String group, String key) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        client.removeNode(String.format("%s/%s/%s", PATH_ROOT, group, key));
        return true;
    }


    @Override
    public void attention(String group, String key, CloudConfigHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudConfigObserverEntity entity = new CloudConfigObserverEntity(group, key, observer);

        client.watchNodeData(String.format("%s/%s/%s", PATH_ROOT, group, key), event -> {
            entity.handle(pull(entity.group, entity.key));
        });
    }

    /**
     * 关闭
     */
    public void close() throws InterruptedException {
        if (client != null) {
            client.close();
        }
    }
}
