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
package org.noear.solon.cloud.extend.quartz;

import org.noear.solon.Solon;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.quartz.service.CloudJobServiceImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.event.AppLoadEndEvent;

/**
 * @author noear
 * @since 1.11
 */
public class QuartzCloudPlugin implements Plugin {
    @Override
    public void start(AppContext context) throws Throwable {
        CloudProps cloudProps = new CloudProps(context, "quartz");

        if (cloudProps.getJobEnable() == false) {
            return;
        }

        //注册Job服务
        CloudManager.register(CloudJobServiceImpl.instance);

//        CloudJobBeanBuilder.getInstance().addBuilder(Job.class, (clz, bw, anno) -> {
//            String name = Utils.annoAlias(anno.value(), anno.name());
//            CloudJobServiceImpl.instance.registerDo(name, anno.cron7x(), anno.description(), ((Job) bw.raw()).getClass());
//        });

        Solon.app().onEvent(AppLoadEndEvent.class, e -> {
            CloudJobServiceImpl.instance.start();
        });
    }
}
