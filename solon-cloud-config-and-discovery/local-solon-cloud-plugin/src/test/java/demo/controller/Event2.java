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

import org.noear.snack.ONode;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.annotation.CloudEvent;
import org.noear.solon.cloud.model.Event;

/**
 * 云端事件（本地实现时，不支持ACK，不支持延时；最好还是引入消息队列的适配框架）
 */
@CloudEvent("demo.event2")
public class Event2 implements CloudEventHandler {
    @Override
    public boolean handle(Event event) throws Throwable {
        System.out.println("云端事件打印: " + ONode.stringify(event));

        if (event.times() > 2) {
            return true;
        } else {
            return false;
        }
    }
}
