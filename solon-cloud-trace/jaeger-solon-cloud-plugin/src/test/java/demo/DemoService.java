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

import org.noear.nami.annotation.NamiMapping;
import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.tracing.Spans;

/**
 * @author noear 2022/5/7 created
 */
@Component
public class DemoService {
    @NamiMapping("hello")
    public String hello() {
        Spans.active(span -> span.setTag("订单", 12));
        //或
        Spans.active().setTag("订单", 12);

        return "hello world";
    }
}
