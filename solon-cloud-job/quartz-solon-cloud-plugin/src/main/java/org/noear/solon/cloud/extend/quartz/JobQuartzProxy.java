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

import org.noear.solon.cloud.model.JobHolder;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.ContextEmpty;
import org.noear.solon.core.handle.ContextUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

/**
 * Quartz job 的执行代理
 *
 * @author noear
 * */
public class JobQuartzProxy implements Job {
    @Override
    public void execute(JobExecutionContext jc) throws JobExecutionException {
        String name = jc.getJobDetail().getKey().getName();
        JobHolder jobHolder = JobManager.getJob(name);

        if (jobHolder != null) {
            Context ctx = Context.current(); //可能是从上层代理已生成, v1.11
            if (ctx == null) {
                ctx = new ContextEmpty();
                ContextUtil.currentSet(ctx);
            }

            //设置请求对象（mvc 时，可以被注入）
            if(ctx instanceof ContextEmpty) {
                ((ContextEmpty) ctx).request(jc);
            }

            for (Map.Entry<String, Object> kv : jc.getJobDetail().getJobDataMap().entrySet()) {
                if (kv.getValue() != null) {
                    ctx.paramMap().put(kv.getKey(), kv.getValue().toString());
                }
            }

            try {
                jobHolder.handle(ctx);
            } catch (Throwable e) {
                throw new JobExecutionException("Job execution failed: " + name, e);
            }
        }
    }
}
