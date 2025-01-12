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

import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.model.Event;
import org.noear.solon.core.handle.Context;
import org.noear.solon.validation.annotation.Valid;
import org.noear.solon.validation.annotation.Whitelist;

import java.util.Date;

/**
 * @author noear 2022/11/22 created
 */
@Valid
@Controller
public class Api1 {
    @Mapping("list1")
    public boolean list1(Context ctx) {
        return CloudClient.list().inListOfIp("whitelist", ctx.realIp());
    }

    @Whitelist
    @Mapping("list2")
    public String list2() {
        return "ok";
    }

    @Mapping("event2")
    public String event2(){
        //发送云端事件
        CloudClient.event().publish(new Event("demo.event2", "test2"));
        return "ok";
    }

    @Mapping("event22")
    public String event22(){
        //发送云端事件
        CloudClient.event().publish(new Event("demo.event22", "test22"));
        return "ok";
    }

    @Mapping("event3")
    public String event3(){
        System.out.println("云端定时事件：event3:" + new Date());

        //发送云端事件
        long currentTimeMillis = System.currentTimeMillis() + 1000 * 5;
        CloudClient.event().publish(new Event("demo.event3", "test3").scheduled(new Date(currentTimeMillis)));
        return "ok";
    }
}
