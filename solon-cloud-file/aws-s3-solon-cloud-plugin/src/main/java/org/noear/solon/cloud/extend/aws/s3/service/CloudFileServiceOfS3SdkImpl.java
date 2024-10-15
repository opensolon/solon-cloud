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
package org.noear.solon.cloud.extend.aws.s3.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.extend.aws.s3.utils.BucketUtils;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.handle.Result;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Date;

/**
 * 云端文件服务（aws s3）
 *
 * @author noear
 * @since 1.3
 */
public class CloudFileServiceOfS3SdkImpl implements CloudFileService {
    private final String bucketDef;
    private final S3Client client;
    private final S3Presigner presigner;

    public S3Client getClient() {
        return client;
    }

    public S3Presigner getPresigner() {
        return presigner;
    }

    public CloudFileServiceOfS3SdkImpl(CloudProps cloudProps) {
        this.bucketDef = cloudProps.getFileBucket();
        this.client = BucketUtils.createClient(cloudProps);
        this.presigner = BucketUtils.createClientPresigner(cloudProps);
    }

    public CloudFileServiceOfS3SdkImpl(String bucketDef, S3Client client, S3Presigner presigner) {
        this.bucketDef = bucketDef;
        this.client = client;
        this.presigner = presigner;
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            return client.headObject(headObjectRequest).sdkHttpResponse().isSuccessful();
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Duration duration) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(duration)
                    .build();

            URL url = presigner.presignGetObject(presignRequest).url();
            return url != null ? url.toString() : null;
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
            // 使用 GetObjectRequest 而不是 GetUrlRequest
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            // 获取对象流
            ResponseInputStream<GetObjectResponse> responseInputStream = client.getObject(getObjectRequest);
            GetObjectResponse response = responseInputStream.response();

            // 获取响应的内容类型和内容大小
            String contentType = response.contentType();
            long contentSize = response.contentLength();

            // 返回新的 Media 对象，包含对象的输入流、内容类型和大小
            return new Media(responseInputStream, contentType, contentSize);
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
            // 构建 PutObjectRequest
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(streamMime)
                    .build();
            // 将 InputStream 转换为 RequestBody
            RequestBody requestBody = RequestBody.fromInputStream(media.body(), media.contentSize());

            // 上传对象
            PutObjectResponse resp = client.putObject(putObjectRequest, requestBody);

            return Result.succeed(resp);
        } catch (Exception ex) {
            throw new CloudFileException(ex);
        }
    }


    @Override
    public Result delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            DeleteObjectResponse resp = client.deleteObject(deleteObjectRequest);
            return Result.succeed(resp);
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }
}
