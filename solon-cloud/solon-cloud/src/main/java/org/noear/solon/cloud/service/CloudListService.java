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
package org.noear.solon.cloud.service;

/**
 * 云端名单列表服务（安全名单，非法名单，黑名单，白名单）
 *
 * @author noear
 * @since 1.3
 */
public interface CloudListService {
    /**
     * 在名单列表中
     *
     * @param names 列表名
     * @param type  检测类型
     * @param value 值
     */
    boolean inList(String names, String type, String value);

    /**
     * 在IP名单列表中
     *
     * @param names 列表名
     * @param ip    Ip
     */
    default boolean inListOfIp(String names, String ip) {
        return inList(names, "ip", ip);
    }


    /**
     * 在IP名单列表中
     *
     * @param ip Ip
     */
    default boolean inListOfServerIp(String ip) {
        return inList("server", "ip", ip);
    }


    /**
     * 在IP名单列表中
     *
     * @param ip Ip
     */
    default boolean inListOfClientIp(String ip) {
        return inList("client", "ip", ip);
    }

    /**
     * 在IP名单列表中
     *
     * @param ip Ip
     */
    default boolean inListOfClientAndServerIp(String ip) {
        return inList("client,server", "ip", ip);
    }

    /**
     * 在手机名单列表中
     *
     * @param names  列表名
     * @param mobile 手机号
     */
    default boolean inListOfMobile(String names, String mobile) {
        return inList(names, "mobile", mobile);
    }

    /**
     * 在域名单列表中
     *
     * @param names  列表名
     * @param domain 域
     */
    default boolean inListOfDomain(String names, String domain) {
        return inList(names, "domain", domain);
    }

    /**
     * 在令牌名单列表中
     *
     * @param names 列表名
     * @param token 令牌
     */
    default boolean inListOfToken(String names, String token) {
        return inList(names, "token", token);
    }

    /**
     * 在Token名单列表中
     *
     * @param token Token
     */
    default boolean inListOfServerToken(String token) {
        return inList("server", "token", token);
    }

}
