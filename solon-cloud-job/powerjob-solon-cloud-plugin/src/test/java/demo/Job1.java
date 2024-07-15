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
package demo;

import org.noear.solon.cloud.CloudJobHandler;
import org.noear.solon.cloud.annotation.CloudJob;
import org.noear.solon.core.handle.Context;
import tech.powerjob.worker.core.processor.TaskContext;

/**
 * 会转为 PowerJobProxy implements BasicProcessor 处理
 *
 * @author noear 2023/1/29 created
 */
@CloudJob("job1")
public class Job1 implements CloudJobHandler {
    @Override
    public void handle(Context ctx) throws Throwable {
        //拿到 TaskContext
        TaskContext taskContext = (TaskContext)ctx.request();
    }
}
