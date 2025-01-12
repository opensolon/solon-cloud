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

import org.noear.solon.cloud.extend.snowflake.impl.SnowflakeId;
import org.noear.solon.cloud.service.CloudIdService;

/**
 * @author noear
 * @since 1.3
 */
public class CloudIdServiceImp implements CloudIdService {

    private SnowflakeId snowFlakeId;

    public CloudIdServiceImp(String dataBlock, long workId, long idStart) {
        snowFlakeId = new SnowflakeId(dataBlock, workId, idStart);
    }

    @Override
    public long generate() {
        return snowFlakeId.nextId();
    }
}
