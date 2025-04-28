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
package org.noear.solon.cloud.extend.local.service;

import org.noear.solon.cloud.CloudJobHandler;
import org.noear.solon.cloud.exception.CloudJobException;
import org.noear.solon.cloud.extend.local.impl.job.CloudJobRunnable;
import org.noear.solon.cloud.extend.local.impl.job.Cron7X;
import org.noear.solon.cloud.extend.local.impl.job.JobManager;
import org.noear.solon.cloud.model.JobHolder;
import org.noear.solon.cloud.service.CloudJobService;
import org.noear.solon.core.util.LogUtil;
import org.noear.solon.logging.utils.TagsMDC;

import java.text.ParseException;

/**
 * 云端定时任务（本地摸拟实现）
 *
 * @author noear
 * @since 1.11
 */
public class CloudJobServiceLocalImpl implements CloudJobService {
    @Override
    public boolean register(String name, String cron7x, String description, CloudJobHandler handler) {
        try {
            JobHolder jobHolder = new JobHolder(name, cron7x, description, handler);
            Cron7X cron7X = Cron7X.parse(cron7x);

            if (cron7X.getInterval() == null) {
                JobManager.add(name, description, cron7x, new CloudJobRunnable(jobHolder));
            } else {
                JobManager.add(name, description, cron7X.getInterval(), new CloudJobRunnable(jobHolder));
            }

            TagsMDC.tag0("CloudJob");
            LogUtil.global().info("CloudJob: Handler registered name:" + name + ", class:" + handler.getClass().getName());
            TagsMDC.tag0("");

            return true;
        } catch (ParseException e) {
            throw new CloudJobException(e);
        }
    }

    @Override
    public boolean isRegistered(String name) {
        return JobManager.contains(name);
    }
}
