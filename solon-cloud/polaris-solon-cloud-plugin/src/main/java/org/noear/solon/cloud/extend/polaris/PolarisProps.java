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
package org.noear.solon.cloud.extend.polaris;

import com.tencent.polaris.factory.ConfigAPIFactory;
import com.tencent.polaris.factory.config.ConfigurationImpl;
import org.noear.solon.Utils;
import org.noear.solon.core.util.ResourceUtil;

import java.net.URL;

/**
 * @author noear
 * @since 1.2
 */
public class PolarisProps {


    private static ConfigurationImpl cfgImpl;

    public static ConfigurationImpl getCfgImpl() {
        if (cfgImpl == null) {
            cfgImpl = (ConfigurationImpl) ConfigAPIFactory.defaultConfig();

            URL cfgUri = ResourceUtil.getResource("polaris.yml");
            if (cfgUri == null) {
                //如果没有配置文件，把持久化去掉
                cfgImpl.getConsumer().getLocalCache().setPersistEnable(false);
                cfgImpl.getConfigFile().getServerConnector().setPersistEnable(false);
            }
        }

        return cfgImpl;
    }
}
