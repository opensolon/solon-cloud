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
package org.noear.solon.cloud.extend.jedis.integration;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.jedis.service.CloudEventServiceJedisImpl;
import org.noear.solon.cloud.extend.jedis.service.CloudLockServiceJedisImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Constants;
import org.noear.solon.core.Plugin;

/**
 * @author noear
 * @since 1.10
 */
public class JedisCloudPlugin implements Plugin {

    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context,"jedis");

        if (cloudProps.getLockEnable() && Utils.isNotEmpty(cloudProps.getLockServer())) {
            CloudLockServiceJedisImpl lockServiceImp = new CloudLockServiceJedisImpl(cloudProps);
            CloudManager.register(lockServiceImp);
        }

        if (cloudProps.getEventEnable() && Utils.isNotEmpty(cloudProps.getEventServer())) {
            CloudEventServiceJedisImpl eventServiceImp = new CloudEventServiceJedisImpl(cloudProps);
            CloudManager.register(eventServiceImp);

            context.lifecycle(Constants.LF_IDX_PLUGIN_BEAN_USES, eventServiceImp);
        }
    }
}
