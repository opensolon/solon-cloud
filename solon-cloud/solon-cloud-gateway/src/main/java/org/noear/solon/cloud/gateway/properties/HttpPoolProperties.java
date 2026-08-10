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
package org.noear.solon.cloud.gateway.properties;

import java.io.Serializable;

/**
 * Http 连接池配置，绑定 solon.cloud.gateway.httpClient.pool.*
 *
 * <ul>
 *   <li>maxConnections ≈ 目标QPS × 平均响应时间(秒)。如 250 ≈ 响应100ms 时 2500 QPS/上游；
 *       总连接数 = Σ(每上游 host)，上游较多时按需调小</li>
 *   <li>maxWaitQueueSize ≈ 目标QPS × 峰值排队时长(秒)。如 1000 ≈ 5000 QPS 下容忍约 200ms 排队，
 *       超限快速失败(503)，防无限排队悬挂（-1=无限制）</li>
 *   <li>maxIdleTime（秒）：空闲回收，配合常见 LB 60s 空闲断开；过大占用连接，过小频繁建连</li>
 *   <li>keepAliveTimeout（秒）：HTTP keep-alive 保持时长</li>
 *   <li>maxPools：按上游 host 拆分的连接池数量上限。超限时淘汰最久未使用的池并关闭其客户端，
 *       防 lb 实例扩缩容/滚动发布/容器 IP 漂移导致 HttpClient 无界累积</li>
 * </ul>
 *
 * @author noear
 * @since 4.0.5
 */
public class HttpPoolProperties implements Serializable {
    private int maxConnections = 250;      //每上游 host 最大连接数：≈QPS×平均响应秒数（见类注释）
    private int maxWaitQueueSize = 1000;   //池等待队列上限：池满且排队超限→快速失败(503)，防无限排队悬挂（-1=无限制）
    private int maxIdleTime = 60;          //空闲超时（秒），配合常见 LB 60s 空闲断开
    private int keepAliveTimeout = 60;     //keep-alive 保持时长（秒）
    private int maxPools = 256;            //连接池数量上限（按上游 host 分池）：超限 LRU 淘汰并关闭，防实例漂移泄漏

    public HttpPoolProperties() {

    }

    /**
     * 获取每上游 host 最大连接数
     */
    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    /**
     * 获取连接池等待队列上限
     */
    public int getMaxWaitQueueSize() {
        return maxWaitQueueSize;
    }

    public void setMaxWaitQueueSize(int maxWaitQueueSize) {
        this.maxWaitQueueSize = maxWaitQueueSize;
    }

    /**
     * 获取空闲超时（秒）
     */
    public int getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    /**
     * 获取 keep-alive 保持时长（秒）
     */
    public int getKeepAliveTimeout() {
        return keepAliveTimeout;
    }

    public void setKeepAliveTimeout(int keepAliveTimeout) {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    /**
     * 获取连接池数量上限（按上游 host 分池，超限 LRU 淘汰并关闭）
     */
    public int getMaxPools() {
        return maxPools;
    }

    public void setMaxPools(int maxPools) {
        this.maxPools = maxPools;
    }
}
