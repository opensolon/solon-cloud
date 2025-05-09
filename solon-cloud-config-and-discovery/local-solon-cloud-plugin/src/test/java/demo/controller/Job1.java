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
package demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudJobHandler;
import org.noear.solon.cloud.annotation.CloudJob;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.core.handle.Context;

import java.util.Date;

/**
 * 云端调度的定时任务（本地实现时，就在本地调试了）
 */
@Slf4j
@CloudJob(name = "job1",cron7x = "* * * * * ?")
public class Job1 implements CloudJobHandler {
    @Override
    public void handle(Context ctx) throws Throwable {
        log.info("云端定时任务：job1:" + new Date());

        //发送云端事件
        CloudClient.event().publish(new Event("demo.event1", "test"));
    }
}
