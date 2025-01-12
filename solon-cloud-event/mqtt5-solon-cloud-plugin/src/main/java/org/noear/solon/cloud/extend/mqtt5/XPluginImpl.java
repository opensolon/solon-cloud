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
package org.noear.solon.cloud.extend.mqtt5;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.mqtt5.service.CloudEventServiceMqtt5;
import org.noear.solon.cloud.extend.mqtt5.service.MqttClientManager;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.LifecycleIndex;
import org.noear.solon.core.Plugin;

/**
 * @author noear
 * @since 2.4
 */
public class XPluginImpl implements Plugin {
    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "mqtt");

        if (Utils.isEmpty(cloudProps.getEventServer())) {
            return;
        }

        if (cloudProps.getEventEnable()) {
            CloudEventServiceMqtt5 eventServiceImp = new CloudEventServiceMqtt5(cloudProps);
            CloudManager.register(eventServiceImp);

            context.wrapAndPut(MqttClientManager.class, eventServiceImp.getClientManager());
            context.lifecycle(LifecycleIndex.PLUGIN_BEAN_USES, eventServiceImp);
        }
    }
}