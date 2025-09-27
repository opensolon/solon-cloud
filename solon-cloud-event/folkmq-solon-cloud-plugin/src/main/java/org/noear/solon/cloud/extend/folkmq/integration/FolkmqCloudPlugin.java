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
package org.noear.solon.cloud.extend.folkmq.integration;

import org.noear.folkmq.client.MqClient;
import org.noear.folkmq.client.MqConsumeListener;
import org.noear.folkmq.client.MqTransactionCheckback;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.folkmq.service.CloudEventServiceFolkMqImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Constants;
import org.noear.solon.core.Plugin;

/**
 * @author noear
 * @since 2.6
 */
public class FolkmqCloudPlugin implements Plugin {
    @Override
    public void start(AppContext context) throws Throwable {
        CloudProps cloudProps = new CloudProps(context, "folkmq");

        if (Utils.isEmpty(cloudProps.getEventServer())) {
            return;
        }

        if (cloudProps.getEventEnable()) {
            CloudEventServiceFolkMqImpl eventServiceImp = new CloudEventServiceFolkMqImpl(cloudProps);
            CloudManager.register(eventServiceImp);

            context.lifecycle(Constants.LF_IDX_PLUGIN_BEAN_USES, eventServiceImp);


            //加入容器
            context.wrapAndPut(MqClient.class, eventServiceImp.getClient());

            //异步获取 MqTransactionCheckback
            context.getBeanAsync(MqTransactionCheckback.class, bean -> {
                eventServiceImp.getClient().transactionCheckback(bean);
            });

            //异步获取 MqConsumeListener
            context.getBeanAsync(MqConsumeListener.class, bean -> {
                eventServiceImp.getClient().listen(bean);
            });
        }
    }
}
