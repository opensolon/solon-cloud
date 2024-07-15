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

import org.noear.solon.Solon;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.extend.file.s3.service.CloudFileServiceImpl;
import org.noear.solon.core.Props;

/**
 * @author 等風來再離開 2022/11/29 created
 */
public class DemoApp {
    public static void main(String[] args) {
        Solon.start(DemoApp.class, args);

        CloudFileServiceImpl fileService = (CloudFileServiceImpl)CloudClient.file();
        fileService.addBucket("xxx", new Props());
    }
}
