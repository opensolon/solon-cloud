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
package org.noear.solon.cloud.extend.nacos.impl;

import com.alibaba.nacos.api.PropertyKeyConst;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.core.Props;

import java.util.Properties;

/**
 * @author noear
 * @since 2.2
 */
public class NacosConfig {
    public static Properties getServiceProperties(CloudProps cloudProps, Properties properties, String server) {
        String username = cloudProps.getUsernameRaw();
        String password = cloudProps.getPasswordRaw();
        String accessKey = cloudProps.getAccessKeyRaw();
        String secretKey = cloudProps.getSecretKeyRaw();

        Props parentProps = cloudProps.getProp();
        parentProps.forEach((k,v)->{
            if(k instanceof String){
                String key = (String) k;

                if(key.startsWith(CloudProps.PREFIX_config) ||
                        key.startsWith(CloudProps.PREFIX_discovery)){
                    return;
                }

                properties.putIfAbsent(key, v);
            }
        });


        properties.putIfAbsent(PropertyKeyConst.SERVER_ADDR, server);

        if (Utils.isNotEmpty(username)) {
            properties.putIfAbsent(PropertyKeyConst.USERNAME, username);
        }

        if (Utils.isNotEmpty(password)) {
            properties.putIfAbsent(PropertyKeyConst.PASSWORD, password);
        }

        if (Utils.isNotEmpty(accessKey)) {
            properties.putIfAbsent(PropertyKeyConst.ACCESS_KEY, username);
        }

        if (Utils.isNotEmpty(secretKey)) {
            properties.putIfAbsent(PropertyKeyConst.SECRET_KEY, password);
        }

        if (Utils.isNotEmpty(cloudProps.getNamespace())) {
            properties.putIfAbsent(PropertyKeyConst.NAMESPACE, cloudProps.getNamespace());
        }

        return properties;
    }
}
