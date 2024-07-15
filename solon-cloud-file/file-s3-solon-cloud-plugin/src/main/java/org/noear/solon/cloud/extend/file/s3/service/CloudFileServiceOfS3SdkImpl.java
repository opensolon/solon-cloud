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
package org.noear.solon.cloud.extend.file.s3.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import org.noear.solon.Utils;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.extend.file.s3.utils.BucketUtils;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.Props;
import org.noear.solon.core.handle.Result;

import java.net.URL;
import java.util.Date;

/**
 * CloudFileService 的远程实现（基于 s3 协议）
 *
 * @author 等風來再離開
 * @since 1.11
 */
public class CloudFileServiceOfS3SdkImpl implements CloudFileService {
    private final String bucketDef;
    private final AmazonS3 client;

    public AmazonS3 getClient() {
        return client;
    }

    public CloudFileServiceOfS3SdkImpl(String bucketDef, Props props) {
        this.bucketDef = bucketDef;
        this.client = BucketUtils.createClient(props);
    }

    public CloudFileServiceOfS3SdkImpl(String bucketDef, AmazonS3 client) {
        this.bucketDef = bucketDef;
        this.client = client;
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            return client.doesObjectExist(bucket, key);
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Date expiration) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            URL url = client.generatePresignedUrl(bucket, key, expiration, HttpMethod.GET);

            if (url == null) {
                return null;
            }

            return url.toString();
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            S3Object obj = client.getObject(bucket, key);

            String contentType = obj.getObjectMetadata().getContentType();
            long contentSize = obj.getObjectMetadata().getContentLength();

            return new Media(obj.getObjectContent(), contentType, contentSize);
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result put(String bucket, String key, Media media) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        String streamMime = media.contentType();
        if (Utils.isEmpty(streamMime)) {
            streamMime = "text/plain; charset=utf-8";
        }

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(streamMime);
            metadata.setContentLength(media.contentSize());

            PutObjectRequest request = new PutObjectRequest(bucket, key, media.body(), metadata);
            request.setCannedAcl(CannedAccessControlList.PublicRead);

            PutObjectResult tmp = client.putObject(request);

            return Result.succeed(tmp);
        } catch (Exception ex) {
            throw new CloudFileException(ex);
        }
    }

    @Override
    public Result delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        client.deleteObject(bucket, key);
        return Result.succeed();
    }
}
