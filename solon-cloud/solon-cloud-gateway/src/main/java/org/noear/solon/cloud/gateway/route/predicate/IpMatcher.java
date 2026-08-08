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
package org.noear.solon.cloud.gateway.route.predicate;

import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.noear.solon.Utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * IP 网段匹配公共工具（RemoteAddr / XForwardedRemoteAddr 谓词共用）
 *
 * <p>关键安全约束：匹配前必须做 IP 字面量判定（isIpLiteral），非字面量（主机名）直接不匹配，
 * 杜绝在事件循环上触发同步 DNS 解析（阻塞攻击向量）。</p>
 *
 * @author noear
 * @since 4.0.5
 */
public class IpMatcher {
    private IpMatcher() {
    }

    /**
     * 构造 IP 网段匹配规则（IPv4/IPv6，支持 CIDR；缺省掩码按地址族全匹配：IPv4=32 / IPv6=128）
     *
     * @param config        谓词配置，如 "192.168.1.0/24" 或 "::1/128"
     * @param predicateName 谓词名称（用于异常信息定位）
     */
    public static IpSubnetFilterRule buildRule(String config, String predicateName) {
        String[] parts = config.split("/");

        try {
            InetAddress address = InetAddress.getByName(parts[0]);

            //获取掩码（缺省按地址族全匹配）
            int mask;
            if (parts.length > 1) {
                mask = Integer.parseInt(parts[1]);
            } else {
                mask = address instanceof Inet4Address ? 32 : 128;
            }

            return new IpSubnetFilterRule(address, mask, IpFilterRuleType.ACCEPT);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(predicateName + " config is wrong: " + config, e);
        }
    }

    /**
     * 是否为 IP 字面量（IPv4/IPv6），非主机名；非字面量不参与匹配，避免同步 DNS
     */
    public static boolean isIpLiteral(String ip) {
        //IPv4 字面量（纯数字，JDK 内建解析）
        if (ip.matches("^\\d{1,3}(\\.\\d{1,3}){3}$")) {
            return true;
        }

        //IPv6 字面量（含冒号；>=0 兼容 "::1" 等冒号开头的压缩写法）
        return ip.indexOf(':') >= 0 && ip.matches("^[0-9a-fA-F:]+$");
    }

    /**
     * 匹配字面量 IP 是否落在规则内（非字面量 / 空值返回 false，不抛异常）
     */
    public static boolean matches(IpSubnetFilterRule rule, String ip) {
        if (rule == null || Utils.isEmpty(ip) || !isIpLiteral(ip)) {
            return false;
        }

        try {
            //ip 已确认为字面量，JDK 内建解析，不会触发 DNS 查询
            InetAddress address = InetAddress.getByName(ip);

            //只匹配ip，所以这里的端口无效
            return rule.matches(new InetSocketAddress(address, 1));
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
