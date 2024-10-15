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
package org.noear.solon.cloud.extend.aws.s3;

import org.noear.solon.cloud.CloudManager;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.extend.aws.s3.service.CloudFileServiceOfS3HttpImpl;
import org.noear.solon.cloud.extend.aws.s3.service.CloudFileServiceOfS3SdkImpl;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Plugin;
import org.noear.solon.core.util.ClassUtil;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * @author noear
 * @since 1.3
 */
public class XPluginImp implements Plugin {
    @Override
    public void start(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "aws.s3");

        if (cloudProps.getFileEnable()) {
            //支持直接使用 AWS 环境（不需要配置）
            if (cloudProps.getProp().size() == 0) {
                //没有任何属性时，必须增加 "..file.enable=true"
                return;
            }

            if (ClassUtil.hasClass(() -> S3Client.class)) {
                CloudManager.register(new CloudFileServiceOfS3SdkImpl(cloudProps));
            } else {
                CloudManager.register(new CloudFileServiceOfS3HttpImpl(cloudProps));
            }
        }
    }
}
