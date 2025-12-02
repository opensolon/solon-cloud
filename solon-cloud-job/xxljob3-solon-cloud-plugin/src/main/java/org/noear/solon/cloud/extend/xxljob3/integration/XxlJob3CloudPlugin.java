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
package org.noear.solon.cloud.extend.xxljob3.integration;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.annotation.CloudJob;
import org.noear.solon.cloud.extend.xxljob3.service.CloudJobServiceImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.BeanWrap;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.event.AppLoadEndEvent;

/**
 * @author noear
 * @since 1.4
 */
public class XxlJob3CloudPlugin implements Plugin {
    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "xxljob");

        if (Utils.isEmpty(cloudProps.getServer())) {
            return;
        }

        if (!cloudProps.getJobEnable()) {
            return;
        }

        //注册 bean 给 XxlJobAutoConfig 用
        BeanWrap beanWrap = context.wrap("xxljob-cloudProps", cloudProps);
        context.putWrap("xxljob-cloudProps", beanWrap);

        ///////////////////////////////////////////////////

        //注册Job服务
        CloudManager.register(new CloudJobServiceImpl());

        //添加 @CloudJob 对 IJobHandler 类的支持 //@since 2.0 //@since 2.9
        context.beanBuilderAdd(CloudJob.class, IJobHandler.class, (clz, bw, anno) -> {
            //支持${xxx}配置
            String name = Solon.cfg().getByTmpl(Utils.annoAlias(anno.value(), anno.name()));
            //提示：不支持CloudJob拦截器
            XxlJobExecutor.registryJobHandler(name, bw.raw());
        });

        //注册构建器和提取器
        context.beanExtractorAdd(XxlJob.class, new XxlJob3Extractor());

        //构建自动配置
        context.beanMake(XxlJob3AutoConfig.class);

        Solon.app().onEvent(AppLoadEndEvent.class, e -> {
            XxlJobExecutor executor = context.getBean(XxlJobExecutor.class);
            executor.start();
        });
    }
}
