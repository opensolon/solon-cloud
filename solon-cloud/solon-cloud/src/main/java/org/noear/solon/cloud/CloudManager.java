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

import org.noear.solon.Utils;
import org.noear.solon.cloud.annotation.CloudConfig;
import org.noear.solon.cloud.annotation.CloudEvent;
import org.noear.solon.cloud.impl.*;
import org.noear.solon.cloud.service.*;
import org.noear.solon.core.util.LogUtil;
import org.noear.solon.core.util.RankEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 云接口管理器
 *
 * @author noear
 * @since 1.2
 */
public class CloudManager {
    private static CloudLoadBalanceFactory loadBalance = new CloudLoadBalanceFactory();
    private static CloudLoadStrategy loadStrategy = new CloudLoadStrategyDefault();

    /**
     * 获取负载均衡
     * */
    protected static CloudLoadBalanceFactory loadBalance() {
        return loadBalance;
    }


    /**
     * 获取负载策略
     */
    protected static CloudLoadStrategy loadStrategy() {
        return loadStrategy;
    }

    /**
     * 设置负载策略
     */
    public static void register(CloudLoadStrategy loadStrategy) {
        if (loadStrategy != null) {
            CloudManager.loadStrategy = loadStrategy;
        }
    }

    //////////////////////


    /**
     * 云端发现服务
     */
    private static CloudDiscoveryService discoveryService;
    /**
     * 云端配置服务
     */
    private static CloudConfigService configService;
    /**
     * 云端事件服务管理
     */
    private static final CloudEventServiceManager eventServiceManager = new CloudEventServiceManagerImpl();
    /**
     * 云端锁服务
     */
    private static CloudLockService lockService;

    /**
     * 云端日志服务
     */
    private static CloudLogService logService;

    /**
     * 云端名单服务
     */
    private static CloudListService listService;

    /**
     * 云端文件服务
     */
    private static CloudFileService fileService;

    /**
     * 去端国际化服务
     */
    private static CloudI18nService i18nService;

    /**
     * 云端断路器服务
     */
    private static CloudBreakerService breakerService;

    /**
     * 云端跟踪服务（链路）
     */
    private static CloudTraceService traceService;

    /**
     * 云端度量服务（监控）
     */
    private static CloudMetricServiceManager metricServiceManager = new CloudMetricServiceManager();

    /**
     * 云端任务服务
     */
    private static CloudJobServiceManager jobServiceManager;

    /**
     * 云端ID生成工厂
     */
    private static CloudIdServiceFactory idServiceFactory;
    private static CloudIdService idServiceDef;


    protected final static Map<CloudConfig, CloudConfigHandler> configHandlerMap = new LinkedHashMap<>();
    protected final static Map<CloudEvent, CloudEventHandler> eventHandlerMap = new LinkedHashMap<>();

    /**
     * 登记配置订阅
     */
    public static void register(CloudConfig anno, CloudConfigHandler handler) {
        configHandlerMap.put(anno, handler);
    }

    /**
     * 登记事件订阅
     */
    public static void register(CloudEvent anno, CloudEventHandler handler) {
        eventHandlerMap.put(anno, handler);
    }


    /**
     * 登记断路器服务
     */
    public static void register(CloudBreakerService service) {
        breakerService = service;
        LogUtil.global().info("Cloud: CloudBreakerService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记配置服务
     */
    public static void register(CloudConfigService service) {
        configService = service;
        LogUtil.global().info("Cloud: CloudConfigService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记注册与发现服务
     */
    public static void register(CloudDiscoveryService service) {
        discoveryService = new CloudDiscoveryServiceProxy(service);
        LogUtil.global().info("Cloud: CloudDiscoveryService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记事件服务
     */
    public static void register(CloudEventServicePlus service) {
        eventServiceManager.register(service);
        if (Utils.isEmpty(service.getChannel())) {
            LogUtil.global().info("Cloud: CloudEventService registered from the " + service.getClass().getTypeName());
        } else {
            LogUtil.global().info("Cloud: CloudEventService registered from the " + service.getClass().getTypeName() + " as &" + service.getChannel());
        }
    }

    /**
     * 登记锁服务
     */
    public static void register(CloudLockService service) {
        lockService = service;
        LogUtil.global().info("Cloud: CloudLockService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记日志服务
     */
    public static void register(CloudLogService service) {
        logService = service;
        LogUtil.global().info("Cloud: CloudLogService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记列表服务
     */
    public static void register(CloudListService service) {
        listService = service;
        LogUtil.global().info("Cloud: CloudListService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记文件服务
     */
    public static void register(CloudFileService service) {
        fileService = service;
        LogUtil.global().info("Cloud: CloudFileService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记国际化服务
     */
    public static void register(CloudI18nService service) {
        i18nService = service;
        LogUtil.global().info("Cloud: CloudI18nService registered from the " + service.getClass().getTypeName());
    }


    /**
     * 登记链路跟踪服务
     */
    public static void register(CloudTraceService service) {
        traceService = service;
        LogUtil.global().info("Cloud: CloudTraceService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记度量服务
     */
    public static void register(CloudMetricService service) {
        metricServiceManager.register(service);
        LogUtil.global().info("Cloud: CloudMetricService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记任务服务
     */
    public static void register(CloudJobService service) {
        jobServiceManager = new CloudJobServiceManagerImpl(service);
        LogUtil.global().info("Cloud: CloudJobService registered from the " + service.getClass().getTypeName());
    }

    /**
     * 登记ID生成工厂
     */
    public static void register(CloudIdServiceFactory factory) {
        idServiceFactory = factory;
        idServiceDef = factory.create();
        LogUtil.global().info("Cloud: CloudIdServiceFactory registered from the " + factory.getClass().getTypeName());
    }

    /**
     * 登记Job拦截器
     */
    public void register(int index, CloudJobInterceptor jobInterceptor) {
        if (jobServiceManager != null) {
            jobServiceManager.addJobInterceptor(index, jobInterceptor);
        }

        LogUtil.global().info("Cloud: CloudJobInterceptor registered from the " + jobInterceptor.getClass().getTypeName());
    }

    protected static CloudBreakerService breakerService() {
        return breakerService;
    }

    protected static CloudConfigService configService() {
        return configService;
    }

    protected static CloudDiscoveryService discoveryService() {
        return discoveryService;
    }

    protected static CloudEventService eventService() {
        return eventServiceManager;
    }

    /**
     * 事件拦截器（仅内部使用）
     */
    public static CloudEventInterceptor eventInterceptor() {
        if (eventServiceManager == null) {
            return null;
        }

        return eventServiceManager.getEventInterceptor();
    }

    protected static CloudLockService lockService() {
        return lockService;
    }

    protected static CloudLogService logService() {
        return logService;
    }

    protected static CloudListService listService() {
        return listService;
    }

    protected static CloudFileService fileService() {
        return fileService;
    }

    protected static CloudI18nService i18nService() {
        return i18nService;
    }

    protected static CloudTraceService traceService() {
        return traceService;
    }

    protected static CloudMetricService metricService() {
        return metricServiceManager;
    }

    protected static CloudIdServiceFactory idServiceFactory() {
        return idServiceFactory;
    }

    protected static CloudIdService idServiceDef() {
        return idServiceDef;
    }

    protected static CloudJobService jobService() {
        return jobServiceManager;
    }

    /**
     * 任务拦截器（仅内部使用）
     */
    public static List<RankEntity<CloudJobInterceptor>> jobInterceptors() {
        if (jobServiceManager == null) {
            return null;
        }

        return jobServiceManager.getJobInterceptors();
    }
}
