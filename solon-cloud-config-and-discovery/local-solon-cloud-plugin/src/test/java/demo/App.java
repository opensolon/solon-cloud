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
import org.noear.solon.core.event.AppLoadEndEvent;
import org.noear.solon.core.event.AppPluginLoadEndEvent;
import org.noear.solon.i18n.I18nUtil;

import java.util.Locale;

/**
 * @author noear 2022/11/21 created
 */
public class App {
    public static void main(String[] args) throws Exception {
        Solon.start(App.class, args, app -> {
            //插件加载完之后
            app.onEvent(AppPluginLoadEndEvent.class, event -> {
                System.out.println("云端配置服务通过配置load的：" + event.app().cfg().get("demo.db1.url"));
            });

            //应用加载完之后
            app.onEvent(AppLoadEndEvent.class, e -> {
                //在 Config::init2 里，用 CloudI18nBundleFactory 替换了本地的
                System.out.println("云端国际化读取：" + I18nUtil.getMessage(Locale.CHINA, "user.name"));
            });
        });
    }
}