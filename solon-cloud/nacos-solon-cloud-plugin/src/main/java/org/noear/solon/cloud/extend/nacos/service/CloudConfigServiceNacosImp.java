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
package org.noear.solon.cloud.extend.nacos.service;

import com.alibaba.nacos.api.config.ConfigFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudConfigHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.nacos.impl.NacosConfig;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.service.CloudConfigObserverEntity;
import org.noear.solon.cloud.service.CloudConfigService;

import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 配置服务适配
 *
 * @author noear
 * @since 1.2
 */
public class CloudConfigServiceNacosImp implements CloudConfigService {
    private ConfigService real;

    public CloudConfigServiceNacosImp(CloudProps cloudProps) {
        Properties properties = NacosConfig.getServiceProperties(cloudProps,
                cloudProps.getProp("config"),
                cloudProps.getConfigServer());

        try {
            real = ConfigFactory.createConfigService(properties);
        } catch (NacosException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * 获取配置
     */
    @Override
    public Config pull(String group, String name) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        //String getConfig(String dataId, String group, long timeoutMs)

        try {
            group = groupReview(group);
            String value = real.getConfig(name, group, 3000);
            return new Config(group, name, value, 0);
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 设置配置
     */
    @Override
    public boolean push(String group, String name, String value) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        //boolean publishConfig(String dataId, String group, String content) throws NacosException

        try {
            group = groupReview(group);
            return real.publishConfig(name, group, value);
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 移除配置
     */
    @Override
    public boolean remove(String group, String name) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        //boolean removeConfig(String dataId, String group) throws NacosException
        try {
            group = groupReview(group);
            return real.removeConfig(name, group);
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 关注配置
     */
    @Override
    public void attention(String group, String name, CloudConfigHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();
        }

        CloudConfigObserverEntity entity = new CloudConfigObserverEntity(group, name, observer);

        try {
            group = groupReview(group);
            real.addListener(name, group, new Listener() {
                @Override
                public Executor getExecutor() {
                    return null;
                }

                @Override
                public void receiveConfigInfo(String value) {
                    entity.handle(new Config(entity.group, entity.key, value, 0));
                }
            });
        } catch (NacosException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String groupReview(String group) {
        if (Utils.isEmpty(group)) {
            return null;
        } else {
            return group;
        }
    }
}