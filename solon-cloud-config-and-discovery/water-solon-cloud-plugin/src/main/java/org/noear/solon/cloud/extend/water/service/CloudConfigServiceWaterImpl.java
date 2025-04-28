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
package org.noear.solon.cloud.extend.water.service;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudConfigHandler;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.service.CloudConfigObserverEntity;
import org.noear.solon.cloud.service.CloudConfigService;
import org.noear.solon.cloud.utils.IntervalUtils;
import org.noear.water.WaterClient;
import org.noear.water.model.ConfigM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * 配置服务
 *
 * @author noear
 * @since 1.2
 */
public class CloudConfigServiceWaterImpl extends TimerTask implements CloudConfigService {
    static final Logger log = LoggerFactory.getLogger(CloudConfigServiceWaterImpl.class);

    private final String DEFAULT_GROUP = "DEFAULT_GROUP";

    private long refreshInterval;

    private Map<String, Config> configMap = new HashMap<>();


    public CloudConfigServiceWaterImpl(CloudProps cloudProps) {
        refreshInterval = IntervalUtils.getInterval(cloudProps.getConfigRefreshInterval("5s"));
    }

    /**
     * 配置刷新间隔时间（仅当isFilesMode时有效）
     */
    public long getRefreshInterval() {
        return refreshInterval;
    }

    @Override
    public void run() {
        try {
            run0();
        } catch (Throwable e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void run0() {
        if (Solon.cfg().isFilesMode()) {
            Set<String> loadGroups = new LinkedHashSet<>();

            try {
                for(CloudConfigObserverEntity entity : observerList){
                    if (loadGroups.contains(entity.group) == false) {
                        loadGroups.add(entity.group);
                        WaterClient.Config.reload(entity.group);
                    }

                    ConfigM cfg = WaterClient.Config.get(entity.group, entity.key);

                    onUpdateDo(entity.group, entity.key, cfg, cfg2 -> {
                        entity.handle(cfg2);
                    });
                }
            } catch (Throwable ex) {

            }
        }
    }

    @Override
    public Config pull(String group, String key) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }

        ConfigM cfg = WaterClient.Config.get(group, key);

        String cfgKey = group + "/" + key;
        Config config = configMap.get(cfgKey);

        if (config == null) {
            config = new Config(group, key, cfg.value, cfg.lastModified);
            configMap.put(cfgKey, config);
        } else if (cfg.lastModified > config.version()) {
            config.updateValue(cfg.value, cfg.lastModified);
        }

        return config;
    }

    @Override
    public boolean push(String group, String key, String value) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }

        try {
            WaterClient.Config.set(group, key, value);
            return true;
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean remove(String group, String key) {
        return false;
    }

    private List<CloudConfigObserverEntity> observerList = new ArrayList<>();

    @Override
    public void attention(String group, String key, CloudConfigHandler observer) {
        if (Utils.isEmpty(group)) {
            group = Solon.cfg().appGroup();

            if (Utils.isEmpty(group)) {
                group = DEFAULT_GROUP;
            }
        }

        CloudConfigObserverEntity entity = new CloudConfigObserverEntity(group, key, observer);
        observerList.add(entity);
    }

    public void onUpdate(String group, String key) {
        if (Utils.isEmpty(group)) {
            return;
        }

        WaterClient.Config.reload(group);
        ConfigM cfg = WaterClient.Config.get(group, key);

        onUpdateDo(group, key, cfg, (cfg2) -> {
            for (CloudConfigObserverEntity entity : observerList) {
                if (group.equals(entity.group) && key.equals(entity.key)) {
                    entity.handle(cfg2);
                }
            }
        });
    }

    private void onUpdateDo(String group, String key, ConfigM cfg, Consumer<Config> consumer) {
        String cfgKey = group + "/" + key;
        Config config = configMap.get(cfgKey);

        if (config == null) {
            config = new Config(group, key, cfg.value, cfg.lastModified);
        } else {
            if (config.version() < cfg.lastModified) {
                config.updateValue(cfg.value, cfg.lastModified);
            } else {
                return;
            }
        }

        consumer.accept(config);
    }
}
