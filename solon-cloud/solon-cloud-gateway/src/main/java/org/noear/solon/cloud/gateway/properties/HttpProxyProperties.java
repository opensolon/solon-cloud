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
 * Http 客户端代理配置，绑定 solon.cloud.gateway.httpClient.proxy.*
 *
 * <ul>
 *   <li>enabled：总开关，默认 false（不配置即直连）</li>
 *   <li>host / port：代理地址，默认端口 8080</li>
 *   <li>type：HTTP / SOCKS4 / SOCKS5，默认 HTTP</li>
 *   <li>username / password：代理认证（HTTP 代理 Basic Auth）</li>
 *   <li>nonProxyHostsPattern：不走代理的主机正则，命中则直连；
 *       如 {@code localhost|127.*|10\\..*}。正则上限 512 字符，非法正则启动期 fail-fast</li>
 * </ul>
 *
 * @author noear
 * @since 4.0.5
 */
public class HttpProxyProperties implements Serializable {
    private boolean enabled = false;   //总开关
    private String host;               //代理地址
    private int port = 8080;           //代理端口
    private String type = "HTTP";      //代理类型：HTTP / SOCKS4 / SOCKS5
    private String username;           //代理认证用户名
    private String password;           //代理认证密码
    private String nonProxyHostsPattern; //不走代理的主机正则

    public HttpProxyProperties() {

    }

    /**
     * 是否启用代理
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取代理地址
     */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /**
     * 获取代理端口
     */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * 获取代理类型（HTTP / SOCKS4 / SOCKS5）
     */
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /**
     * 获取代理认证用户名
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 获取代理认证密码
     */
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * 获取不走代理的主机正则（命中直连）
     */
    public String getNonProxyHostsPattern() {
        return nonProxyHostsPattern;
    }

    public void setNonProxyHostsPattern(String nonProxyHostsPattern) {
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }
}
