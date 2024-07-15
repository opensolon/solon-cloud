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

import org.noear.solon.cloud.annotation.CloudJob;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;

/**
 * 没有名字的 job ，使用全类名调度
 *
 * @author noear 2023/1/29 created
 */
@CloudJob
public class Job4 implements BroadcastProcessor {
    @Override
    public ProcessResult process(TaskContext taskContext) throws Exception {
        return null;
    }
}
