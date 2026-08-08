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
 * 超时属性（单位：秒）
 *
 * <p>三个超时分工与取值参考：
 * <ul>
 *   <li>connectTimeout：连接建立超时。内部网络建议 3~10s</li>
 *   <li>requestTimeout：等待上游响应头超时。内部 API 响应头 P99&lt;1s 时 10s 足够宽松；含慢接口(&gt;10s)请调大</li>
 *   <li>responseTimeout：网关级整体完成兜底（覆盖 filter 链与响应流）。普通 API 60s 足够；SSE/大文件流式请调大或设 0 禁用</li>
 * </ul>
 *
 * @author noear
 * @since 2.9
 */
public class TimeoutProperties implements Serializable {
    private int connectTimeout = 10;    //连接建立超时（秒）
    private int requestTimeout = 10;    //等待上游响应头超时（秒）
    private int responseTimeout = 60;   //整体完成兜底超时（秒），0=禁用（原默认 1800s 过长，收敛为 60s）

    public TimeoutProperties() {

    }

    public TimeoutProperties(int timeout) {
        this(timeout, timeout, timeout);
    }

    public TimeoutProperties(int connectTimeout, int requestTimeout, int responseTimeout) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.responseTimeout = responseTimeout;
    }

    /**
     * 连接超时
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * 请求超时
     */
    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * 响应超时
     */
    public int getResponseTimeout() {
        return responseTimeout;
    }

    public void setResponseTimeout(int responseTimeout) {
        this.responseTimeout = responseTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}