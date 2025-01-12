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
package org.noear.solon.cloud.extend.snowflake.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.service.CloudIdService;
import org.noear.solon.cloud.service.CloudIdServiceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author noear
 * @since 1.3
 */
public class CloudIdServiceFactoryImp implements CloudIdServiceFactory {
    long idStart;
    long workId;

    public CloudIdServiceFactoryImp(CloudProps cloudProps) {
        this.idStart = cloudProps.getIdStart();
        this.workId = Long.parseLong(cloudProps.getValue("id.workId", "0"));
    }


    private Map<String, CloudIdService> cached = new HashMap<>();

    @Override
    public CloudIdService create(String group, String service) {
        String block = group + "_" + service;
        CloudIdService tmp = cached.get(block);

        if (tmp == null) {
            Utils.locker().lock();

            try {
                tmp = cached.get(block);
                if (tmp == null) {
                    tmp = new CloudIdServiceImp(block, workId, idStart);
                    cached.put(block, tmp);
                }
            } finally {
                Utils.locker().unlock();
            }
        }

        return tmp;
    }
}
