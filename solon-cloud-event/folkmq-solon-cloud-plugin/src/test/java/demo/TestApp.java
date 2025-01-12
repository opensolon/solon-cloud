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
package demo;

import org.noear.solon.Solon;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.CloudEventHandler;
import org.noear.solon.cloud.annotation.CloudEvent;
import org.noear.solon.cloud.model.Event;

/**
 * @author noear
 * @since 2.6
 */
@CloudEvent("demo")
public class TestApp implements CloudEventHandler {
    public static void main(String[] args) {
        Solon.start(TestApp.class, args);

        CloudClient.event().publish(new Event("demo", "demo!!!!!!"));
    }

    @Override
    public boolean handle(Event event) throws Throwable {
        System.out.println("打印：" + event.content());
        return true;
    }
}
