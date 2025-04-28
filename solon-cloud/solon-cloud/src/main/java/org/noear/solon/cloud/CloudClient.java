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
package org.noear.solon.cloud;

import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.impl.CloudLoadBalanceFactory;
import org.noear.solon.cloud.impl.CloudLoadStrategy;
import org.noear.solon.cloud.model.Config;
import org.noear.solon.cloud.model.Instance;
import org.noear.solon.cloud.service.*;
import org.noear.solon.core.Signal;
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.util.LogUtil;

import java.util.Properties;

/**
 * 云操作客户端
 *
 * @author noear
 * @since 1.2
 */
public class CloudClient {
    private static boolean enableConfig = true;
    private static boolean enableEvent = true;
    private static boolean enableBreaker = true;
    private static boolean enableJob = true;

    /**
     * 是否启用 @CloudConfig 注解
     */
    public static boolean enableConfig() {
        return enableConfig;
    }

    /**
     * 是否启用 @CloudEvent 注解
     */
    public static boolean enableEvent() {
        return enableEvent;
    }

    /**
     * 是否启用 @CloudBreaker 注解
     */
    public static boolean enableBreaker() {
        return enableBreaker;
    }

    /**
     * 是否启用 @CloudJob 注解
     */
    public static boolean enableJob() {
        return enableJob;
    }

    /**
     * 配置：是否启用 @CloudConfig 注解
     */
    public static void enableConfig(boolean enable) {
        enableConfig = enable;
    }

    /**
     * 配置：是否启用 @CloudEvent 注解
     */
    public static void enableEvent(boolean enable) {
        enableEvent = enable;
    }

    /**
     * 配置：是否启用 @CloudBreaker 注解
     */
    public static void enableBreaker(boolean enable) {
        enableBreaker = enable;
    }

    /**
     * 配置：是否启用 @CloudJob 注解
     */
    public static void enableJob(boolean enable) {
        enableJob = enable;
    }


    /**
     * 获取 负载均衡工厂
     */
    public static CloudLoadBalanceFactory loadBalance() {
        return CloudManager.loadBalance();
    }

    /**
     * 获取 负载策略
     */
    public static CloudLoadStrategy loadStrategy() {
        return CloudManager.loadStrategy();
    }

    /**
     * 获取 云端断路器服务
     */
    public static CloudBreakerService breaker() {
        return CloudManager.breakerService();
    }


    /**
     * 获取 云端配置服务
     */
    public static CloudConfigService config() {
        return CloudManager.configService();
    }

    /**
     * 云端配置服务，加载默认配置
     */
    public static void configLoad(String group, String key) {
        if (CloudClient.config() == null) {
            return;
        }

        if (Utils.isNotEmpty(key)) {
            Config config = CloudClient.config().pull(group, key);

            if (config != null && Utils.isNotEmpty(config.value())) {
                Properties properties = config.toProps();
                Solon.cfg().loadAdd(properties);
            }

            //关注实时更新
            CloudClient.config().attention(group, key, (cfg) -> {
                Properties properties = cfg.toProps();
                Solon.cfg().loadAdd(properties);
            });
        }
    }

    /**
     * 云端配置服务，加载默认配置
     */
    public static void configLoad(String groupKeySet) {
        if (CloudClient.config() == null) {
            return;
        }

        if (Utils.isNotEmpty(groupKeySet)) {
            String[] gkAry = groupKeySet.split(",");
            for (String gkStr : gkAry) {
                String[] gk = null;
                if (gkStr.contains("::")) {
                    gk = gkStr.split("::"); //将弃用：water::water, by 2021-11-13
                } else {
                    gk = gkStr.split(":"); //支持 water:water
                }

                if (gk.length == 2) {
                    configLoad(gk[0], gk[1]);
                } else {
                    configLoad(Solon.cfg().appGroup(), gk[0]);
                }
            }
        }
    }

    /**
     * 获取 云端发现服务
     */
    public static CloudDiscoveryService discovery() {
        return CloudManager.discoveryService();
    }

    /**
     * 云端发现服务，推送本地服务（即注册）
     */
    public static void discoveryPush() {
        if (CloudClient.discovery() == null) {
            return;
        }

        if (Utils.isEmpty(Solon.cfg().appName())) {
            return;
        }

        Solon.app().onEvent(AppLoadEndEvent.class, (event) -> {
            for (Signal signal : Solon.app().signals()) {
                Instance instance = Instance.localNew(signal);
                CloudClient.discovery().register(Solon.cfg().appGroup(), instance);
                LogUtil.global().info("Cloud: Service registered " + instance.service() + "@" + instance.uri());
            }
        });

        Solon.app().onEvent(Signal.class, signal -> {
            Instance instance = Instance.localNew(signal);
            CloudClient.discovery().register(Solon.cfg().appGroup(), instance);
            LogUtil.global().info("Cloud: Service registered " + instance.service() + "@" + instance.uri());
        });
    }

    /**
     * 获取 云端事件服务
     */
    public static CloudEventService event() {
        return CloudManager.eventService();
    }

    /**
     * 获取 云端锁服务
     */
    public static CloudLockService lock() {
        return CloudManager.lockService();
    }

    /**
     * 获取 云端日志服务
     */
    public static CloudLogService log() {
        return CloudManager.logService();
    }

    /**
     * 获取 云端链路跟踪服务
     */
    public static CloudTraceService trace() {
        return CloudManager.traceService();
    }

    /**
     * 获取 云端度量服务
     */
    public static CloudMetricService metric() {
        return CloudManager.metricService();
    }

    /**
     * 获取 云端名单列表服务
     */
    public static CloudListService list() {
        return CloudManager.listService();
    }

    /**
     * 获取 云端文件服务
     */
    public static CloudFileService file() {
        return CloudManager.fileService();
    }

    /**
     * 获取 云端国际化服务
     */
    public static CloudI18nService i18n() {
        return CloudManager.i18nService();
    }

    /**
     * 获取 云端ID服务
     */
    public static CloudIdService idService(String group, String service) {
        return CloudManager.idServiceFactory().create(group, service);
    }

    /**
     * 获取 云端ID服务
     */
    public static CloudIdService id() {
        return CloudManager.idServiceDef();
    }

    /**
     * 获取 云端Job服务
     */
    public static CloudJobService job() {
        return CloudManager.jobService();
    }
}