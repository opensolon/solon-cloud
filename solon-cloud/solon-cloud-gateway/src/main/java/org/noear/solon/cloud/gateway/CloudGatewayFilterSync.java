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
package org.noear.solon.cloud.gateway;

import org.noear.solon.cloud.gateway.exchange.ExFilterSync;

/**
 * 分布式网关过滤器同步形态（用于对接同步 io）
 *
 * @author noear
 * @since 3.1
 */
@FunctionalInterface
public interface CloudGatewayFilterSync extends CloudGatewayFilter, ExFilterSync {

}
