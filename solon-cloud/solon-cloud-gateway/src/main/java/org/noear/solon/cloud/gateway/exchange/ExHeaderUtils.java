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
package org.noear.solon.cloud.gateway.exchange;

import org.noear.solon.Utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 逐跳头处理工具
 *
 * <p>HTTP 请求/响应转发时的逐跳头剥离与 Connection 头声明 token 动态解析
 * （RFC 7230 §6.1），供 Http / WebSocket 路由处理器共用，避免两份列表重复维护。</p>
 *
 * @author noear
 * @since 4.0.6
 */
public class ExHeaderUtils {
    /**
     * 标准逐跳头（RFC 7230 §6.1）+ 非标准 Proxy-Connection
     *
     * <p>不可变集合，仅作 contains 查询。</p>
     */
    public static final Set<String> HOP_BY_HOP_HEADERS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "connection",
            "keep-alive",
            "proxy-authenticate",
            "proxy-authorization",
            "te",
            "trailer",
            "transfer-encoding",
            "upgrade",
            "proxy-connection")));

    private ExHeaderUtils() {
    }

    /**
     * 构建逐跳头剥离集合（静态逐跳头 + Connection 头显式声明的 token）
     *
     * <p>无 Connection 声明时直接返回共享静态集合（只读约定，调用方仅可 contains 查询），
     * 避免每请求新建集合；有声明时才新建并追加 token（Connection 声明的 token 按 RFC 视为逐跳头）。</p>
     *
     * @param connectionHeader Connection 头原始值（可为 null）
     */
    public static Set<String> buildSkipHeaders(String connectionHeader) {
        return buildSkipHeaders(connectionHeader, HOP_BY_HOP_HEADERS);
    }

    /**
     * 构建剥离集合（基础集合 + Connection 头显式声明的 token）
     *
     * @param connectionHeader Connection 头原始值（可为 null）
     * @param baseHeaders      基础剥离集合（如 WS 握手专用集合；调用方需保证其只读）
     */
    public static Set<String> buildSkipHeaders(String connectionHeader, Set<String> baseHeaders) {
        if (Utils.isEmpty(connectionHeader)) {
            return baseHeaders;
        }

        Set<String> skipHeaders = new HashSet<>(baseHeaders);
        for (String token : connectionHeader.split(",")) {
            String t = token.trim().toLowerCase();
            if (Utils.isNotEmpty(t)) {
                skipHeaders.add(t);
            }
        }

        return skipHeaders;
    }
}
