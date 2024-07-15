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
package demo.config;

import org.noear.solon.Utils;
import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.core.handle.Context;
import org.noear.solon.validation.annotation.Whitelist;
import org.noear.solon.validation.annotation.WhitelistChecker;

/**
 * 提供 Whitelist 的检查能力
 */
@Component
public class ApiWhitelistChecker implements WhitelistChecker {
    @Override
    public boolean check(Whitelist anno, Context ctx) {
        String listName = anno.value();
        if (Utils.isEmpty(listName)) {
            listName = "whitelist";
        }

        return CloudClient.list().inListOfIp(listName, ctx.realIp());
    }
}
