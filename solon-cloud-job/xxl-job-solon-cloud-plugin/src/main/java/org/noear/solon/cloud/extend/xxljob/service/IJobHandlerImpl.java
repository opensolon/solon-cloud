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
package org.noear.solon.cloud.extend.xxljob.service;

import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.handler.IJobHandler;
import org.noear.solon.cloud.model.JobHolder;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.core.handle.ContextUtil;

/**
 * @author noear
 * @since 1.4
 * @since 1.11
 */
class IJobHandlerImpl extends IJobHandler {
    JobHolder jobHolder;

    public IJobHandlerImpl(JobHolder jobHolder) {
        this.jobHolder = jobHolder;
    }

    @Override
    public void execute() throws Exception {
        Context ctx = Context.current(); //可能是从上层代理已生成, v1.11
        if (ctx == null) {
            ctx = new ContextEmpty();
            ContextUtil.currentSet(ctx);
        }

        XxlJobContext jobContext = XxlJobContext.getXxlJobContext();

        if(jobContext != null) {
            //设置请求对象（mvc 时，可以被注入）
            if (ctx instanceof ContextEmpty) {
                ((ContextEmpty) ctx).request(jobContext);
            }

            //long jobId, String jobParam, String jobLogFileName, int shardIndex, int shardTotal
            ctx.paramMap().put("jobId", String.valueOf(jobContext.getJobId()));
            ctx.paramMap().put("jobParam", jobContext.getJobParam());
            ctx.paramMap().put("jobLogFileName", jobContext.getJobLogFileName());
            ctx.paramMap().put("shardIndex", String.valueOf(jobContext.getShardIndex()));
            ctx.paramMap().put("shardTotal", String.valueOf(jobContext.getShardTotal()));
        }


        try {
            jobHolder.handle(ctx);
        } catch (Throwable e) {
            if (e instanceof Exception) {
                throw (Exception) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}