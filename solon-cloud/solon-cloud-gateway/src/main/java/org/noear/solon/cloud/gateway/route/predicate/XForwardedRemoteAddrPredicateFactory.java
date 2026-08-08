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

import io.netty.handler.ipfilter.IpSubnetFilterRule;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExPredicate;
import org.noear.solon.cloud.gateway.route.RoutePredicateFactory;

/**
 * 路由 XForwardedRemoteAddr 匹配检测器（信任客户端转发头）
 *
 * <p>不信任客户端头、匹配纯 TCP 对端地址请用 {@link RemoteAddrPredicateFactory}。</p>
 *
 * @author wfm
 * @since 2.9
 * @since 4.0.5
 */
public class XForwardedRemoteAddrPredicateFactory implements RoutePredicateFactory {
    @Override
    public String prefix() {
        return "XForwardedRemoteAddr";
    }

    @Override
    public ExPredicate create(String config) {
        return new XForwardedRemoteAddrPredicate(config);
    }

    public static class XForwardedRemoteAddrPredicate implements ExPredicate {
        private final IpSubnetFilterRule rule;

        /**
         * @param config (XForwardedRemoteAddr=192.168.1.1/24)
         */
        public XForwardedRemoteAddrPredicate(String config) {
            if (Utils.isBlank(config)) {
                throw new IllegalArgumentException("XForwardedRemoteAddr config cannot be blank");
            }

            rule = IpMatcher.buildRule(config, "XForwardedRemoteAddr");
        }

        @Override
        public boolean test(ExContext ctx) {
            String ip = ctx.realIp();

            //非 IP 字面量（如主机名）直接不匹配，避免在事件循环上触发同步 DNS 解析
            return IpMatcher.matches(rule, ip);
        }
    }
}
