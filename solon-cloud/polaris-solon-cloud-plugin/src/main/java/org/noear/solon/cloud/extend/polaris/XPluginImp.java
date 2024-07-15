/*
 * Copyright 2017-2024 noear.org and authors
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
package org.noear.solon.cloud.extend.polaris;

import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.polaris.service.CloudConfigServicePolarisImp;
import org.noear.solon.cloud.extend.polaris.service.CloudDiscoveryServicePolarisImp;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

/**
 * @author 何荣振
 * @since 1.11
 */
public class XPluginImp implements Plugin {
    CloudConfigServicePolarisImp cloudConfigServicePolarisImp;
    CloudDiscoveryServicePolarisImp cloudDiscoveryServicePolarisImp;

    @Override
    public void start(AppContext context) throws Throwable {
        CloudProps cloudProps = new CloudProps(context,"polaris");

        //1.登记配置服务
        if (cloudProps.getConfigEnable()) {
            cloudConfigServicePolarisImp = new CloudConfigServicePolarisImp(cloudProps);
            CloudManager.register(cloudConfigServicePolarisImp);

            //1.1.加载配置
            CloudClient.configLoad(cloudProps.getConfigLoad());
        }

        //2.登记发现服务
        if (cloudProps.getDiscoveryEnable()) {
            cloudDiscoveryServicePolarisImp = new CloudDiscoveryServicePolarisImp(cloudProps);
            CloudManager.register(cloudDiscoveryServicePolarisImp);
        }
    }

    @Override
    public void stop() throws Throwable {
        if (cloudConfigServicePolarisImp != null) {
            cloudConfigServicePolarisImp.close();
        }

        if (cloudDiscoveryServicePolarisImp != null) {
            cloudDiscoveryServicePolarisImp.close();
        }
    }
}
