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
package org.noear.solon.cloud.extend.zipkin.integration;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.zipkin.service.ZipkinTracerFactory;
import org.noear.solon.cloud.tracing.TracingManager;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;

/**
 * @author blackbear2003
 * @since 2.3
 */
public class XPluginImp implements Plugin {
    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "zipkin");

        if (cloudProps.getTraceEnable() == false) {
            return;
        }

        if (Utils.isEmpty(cloudProps.getServer())) {
            return;
        }

        TracingManager.enable(cloudProps.getTraceExclude());
        TracingManager.register(new ZipkinTracerFactory(cloudProps));
    }
}
