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
import org.noear.solon.cloud.CloudConfigHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudConfigException;
import org.noear.solon.cloud.extend.local.impl.CloudLocalUtils;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.service.CloudConfigService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 云端配置（本地摸拟实现）
 *
 * @author noear
 * @since 1.11
 */
public class CloudConfigServiceLocalImpl implements CloudConfigService {
    static final String DEFAULT_GROUP = "DEFAULT_GROUP";
    static final String CONFIG_KEY_FORMAT = "config/%s_%s";

    private final Map<String, Config> configMap = new HashMap<>();

    private final String server;

    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public CloudConfigServiceLocalImpl(CloudProps cloudProps) {
        this.server = cloudProps.getServer();
    }

    @Override
    public Config pull(String group, String name) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }


        String configKey = String.format(CONFIG_KEY_FORMAT, group, name);
        Config configVal = configMap.get(configKey);

        SYNC_LOCK.lock();
        try {
            if (configVal == null) {
                configVal = configMap.get(configKey);

                if (configVal == null) {
                    try {
                        String value2 = CloudLocalUtils.getValue(server, configKey);

                        configVal = new Config(group, name, value2, 0);
                        configMap.put(configKey, configVal);
                    } catch (IOException e) {
                        throw new CloudConfigException(e);
                    }
                }
            }
        } finally {
            SYNC_LOCK.unlock();
        }

        return configVal;
    }

    @Override
    public boolean push(String group, String name, String value) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }

        String configKey = String.format(CONFIG_KEY_FORMAT, group, name);
        Config configVal = pull(group, name);

        SYNC_LOCK.lock();
        try {
            if (configVal == null) {
                configVal = new Config(group, name, value, 0);
                configMap.put(configKey, configVal);
            }

            if (configVal != null) {
                configVal.updateValue(value, configVal.version() + 1);
            }
        } finally {
            SYNC_LOCK.unlock();
        }

        return true;
    }

    @Override
    public boolean remove(String group, String name) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }

        String configKey = String.format(CONFIG_KEY_FORMAT, group, name);
        configMap.remove(configKey);
        return true;
    }

    @Override
    public void attention(String group, String name, CloudConfigHandler observer) {

    }
}