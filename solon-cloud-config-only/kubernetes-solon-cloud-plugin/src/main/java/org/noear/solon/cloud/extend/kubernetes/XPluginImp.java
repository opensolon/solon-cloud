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
package org.noear.solon.cloud.extend.kubernetes;


import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.kubernetes.service.CloudConfigServiceK8sImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

import java.util.Timer;


/**
 * @author noear
 * @since 1.10
 */
public class XPluginImp implements Plugin {
    private Timer clientTimer = new Timer();

    /*
    https://support.huaweicloud.com/sdkreference-cci/cci_09_0004.html
    https://github.com/kubernetes-client/java/wiki/3.-Code-Examples
    */

    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "kubernetes");

        if (Utils.isEmpty(cloudProps.getServer())) {
            return;
        }

        //1.登记配置服务
        if (cloudProps.getConfigEnable()) {
            CloudConfigServiceK8sImpl serviceImp = new CloudConfigServiceK8sImpl(cloudProps);
            CloudManager.register(serviceImp);

            if (serviceImp.getRefreshInterval() > 0) {
                long interval = serviceImp.getRefreshInterval();
                clientTimer.schedule(serviceImp, interval, interval);
            }

            //1.1.加载配置
            CloudClient.configLoad(cloudProps.getConfigLoad());
        }
    }
}
