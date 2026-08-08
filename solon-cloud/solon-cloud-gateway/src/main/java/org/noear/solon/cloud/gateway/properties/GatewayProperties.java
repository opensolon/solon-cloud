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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分布式网关配置属性
 *
 * @author noear
 * @since 2.9
 */
public class GatewayProperties implements Serializable {
    public static final String SOLON_CLOUD_GATEWAY = "solon.cloud.gateway";
    /**
     * 发现配置
     */
    private DiscoverProperties discover = new DiscoverProperties();

    /**
     * 路由记录
     */
    private List<RouteProperties> routes = new ArrayList<>();

    /**
     * Http 客户端配置（超时 + 连接池 + WebSocket），绑定 solon.cloud.gateway.httpClient.*
     */
    private HttpClientProperties httpClient = new HttpClientProperties();

    /**
     * X-Forwarded-* 出站头生成配置，绑定 solon.cloud.gateway.xForwarded.*
     */
    private XForwardedProperties xForwarded = new XForwardedProperties();

    /**
     * 默认路由过滤器（每个路由器加上）
     */
    private List<String> defaultFilters;

    /**
     * 谓词工厂启用开关，绑定 solon.cloud.gateway.predicate.*（键=工厂 prefix，缺省 true）
     * 如 {@code predicate.remote-addr: false} 关闭 RemoteAddr 谓词。
     * 关闭后该谓词配置不再生效（构建返回 null，路由跳过），不报错。
     */
    private Map<String, Boolean> predicate = new HashMap<>();

    /**
     * 过滤器工厂启用开关，绑定 solon.cloud.gateway.filter.*（键=工厂 prefix，缺省 true）
     * 如 {@code filter.strip-prefix: false} 关闭 StripPrefix 过滤器。
     * 关闭后该过滤器配置不再生效（构建返回 null，路由跳过），不报错。
     */
    private Map<String, Boolean> filter = new HashMap<>();

    /**
     * 发现配置
     */
    public DiscoverProperties getDiscover() {
        return discover;
    }

    /**
     * 路由记录
     */
    public List<RouteProperties> getRoutes() {
        return routes;
    }

    /**
     * 默认路由过滤器（每个路由器加上）
     */
    public List<String> getDefaultFilters() {
        return defaultFilters;
    }

    /**
     * 谓词工厂启用开关（键=工厂 prefix，缺省 true）
     */
    public Map<String, Boolean> getPredicate() {
        return predicate;
    }

    public void setPredicate(Map<String, Boolean> predicate) {
        this.predicate = predicate;
    }

    /**
     * 过滤器工厂启用开关（键=工厂 prefix，缺省 true）
     */
    public Map<String, Boolean> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, Boolean> filter) {
        this.filter = filter;
    }

    /**
     * Http 客户端配置
     */
    public HttpClientProperties getHttpClient() {
        return httpClient;
    }

    /**
     * X-Forwarded-* 出站头生成配置
     */
    public XForwardedProperties getXForwarded() {
        return xForwarded;
    }
}