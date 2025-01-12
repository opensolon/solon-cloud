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
package org.noear.solon.cloud.extend.zookeeper;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.zookeeper.service.CloudConfigServiceZkImp;
import org.noear.solon.cloud.extend.zookeeper.service.CloudDiscoveryServiceZkImp;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

/**
 * @author noear
 * @since 1.3
 */
public class XPluginImp implements Plugin {

    CloudConfigServiceZkImp configServiceZkImp;
    CloudDiscoveryServiceZkImp discoveryServiceZkImp;

    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context,"zookeeper");

        if (Utils.isEmpty(cloudProps.getServer())) {
            return;
        }

        //1.登记配置服务
        if (cloudProps.getConfigEnable()) {
            configServiceZkImp = new CloudConfigServiceZkImp(cloudProps);
            CloudManager.register(configServiceZkImp);

            //1.1.加载配置
            CloudClient.configLoad(cloudProps.getConfigLoad());
        }

        //2.登记发现服务
        if (cloudProps.getDiscoveryEnable()) {
            discoveryServiceZkImp = new CloudDiscoveryServiceZkImp(cloudProps);
            CloudManager.register(discoveryServiceZkImp);
        }
    }

    @Override
    public void stop() throws Throwable {
        if (configServiceZkImp != null) {
            configServiceZkImp.close();
        }

        if (discoveryServiceZkImp != null) {
            discoveryServiceZkImp.close();
        }
    }
}
