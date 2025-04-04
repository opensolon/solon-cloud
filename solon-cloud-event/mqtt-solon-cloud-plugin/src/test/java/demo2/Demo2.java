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
package demo2;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;

/**
 * @author noear 2023/7/9 created
 */
@Component
public class Demo2 {

    @Inject
    MqttClient mqttClient;
}
