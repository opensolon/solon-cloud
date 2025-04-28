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
package org.noear.solon.cloud.extend.water.integration.http;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.extend.water.WaterProps;
import org.noear.solon.cloud.extend.water.service.CloudJobServiceWaterImpl;
import org.noear.solon.cloud.model.JobHolder;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Handler;
import org.noear.water.WW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 任务调度处理（用令牌的形式实现安全）//中频
 *
 * @author noear
 * @since 1.4
 */
public class HandlerJob implements Handler {
    static final Logger log = LoggerFactory.getLogger(HandlerJob.class);

    @Override
    public void handle(Context ctx) throws Throwable {
        String token = ctx.headerOrDefault(WaterProps.http_header_token, "");

        //调用任务必须要有server token
        if (authServerToken(token)) {
            String name = ctx.header(WW.http_header_job);
            if(Utils.isEmpty(name)){
                name = ctx.param("name");//兼容旧版
            }

            handleDo(ctx, name);
        }else{
            ctx.status(400);
            ctx.output("Invalid server token!");
        }
    }

    private void handleDo(Context ctx, String name) {
        JobHolder jobHolder = CloudJobServiceWaterImpl.instance.get(name);

        if (jobHolder == null) {
            ctx.status(400);
            if (Utils.isEmpty(name)) {
                ctx.output("CloudJob need the name parameter");
            } else {
                ctx.output("CloudJob[" + name + "] no exists");
            }
        } else {
            try {
                jobHolder.handle(ctx);
                ctx.output("OK");
            } catch (Throwable e) {
                e = Utils.throwableUnwrap(e);
                log.warn(e.getMessage(), e);
                ctx.status(500);
                ctx.output(e);
            }
        }
    }

    /**
     * 验证安全性（基于token）
     */
    private boolean authServerToken(String token) {
        return CloudClient.list().inListOfServerToken(token);
    }
}