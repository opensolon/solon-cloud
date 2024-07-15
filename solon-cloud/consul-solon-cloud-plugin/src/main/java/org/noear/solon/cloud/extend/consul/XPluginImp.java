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
package org.noear.solon.cloud.extend.consul;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.core.*;
import org.noear.solon.cloud.extend.consul.service.CloudConfigServiceConsulImpl;
import org.noear.solon.cloud.extend.consul.service.CloudDiscoveryServiceConsulImpl;

import java.util.*;

/**
 * 集成Consul,配置 application.properties
 *
 * @author 夜の孤城
 * @since 1.2
 * */
public class XPluginImp implements Plugin {
    private Timer clientTimer = new Timer();


    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context,"consul");

        if (Utils.isEmpty(cloudProps.getServer())) {
            return;
        }

        //1.登记配置服务
        if (cloudProps.getConfigEnable()) {
            CloudConfigServiceConsulImpl serviceImp = new CloudConfigServiceConsulImpl(cloudProps);
            CloudManager.register(serviceImp);

            if (serviceImp.getRefreshInterval() > 0) {
                long interval = serviceImp.getRefreshInterval();
                clientTimer.schedule(serviceImp, interval, interval);
            }

            //1.1.加载配置
            CloudClient.configLoad(cloudProps.getConfigLoad());
        }

        //2.登记发现服务
        if (cloudProps.getDiscoveryEnable()) {
            CloudDiscoveryServiceConsulImpl serviceImp = new CloudDiscoveryServiceConsulImpl(cloudProps);
            CloudManager.register(serviceImp);

            //运行一次，拉取服务列表
            serviceImp.run();

            if (serviceImp.getRefreshInterval() > 0) {
                long interval = serviceImp.getRefreshInterval();
                clientTimer.schedule(serviceImp, interval, interval);
            }
        }
    }

    @Override
    public void prestop() throws Throwable {
        if (clientTimer != null) {
            clientTimer.cancel();
        }
    }
}
