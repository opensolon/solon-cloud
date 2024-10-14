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
package org.noear.solon.cloud.extend.aws.s3.utils;

import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.core.Props;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;

/**
 * 存储桶工具类
 *
 * @author 等風來再離開
 * @author noear
 * @since 1.11
 */
public class BucketUtils {
    /***
     * 创建客户端
     * */
    public static S3Client createClient(CloudProps cloudProps) {
        String endpoint = cloudProps.getFileEndpoint();
        String regionId = cloudProps.getFileRegionId();

        String accessKey = cloudProps.getFileAccessKey();
        String secretKey = cloudProps.getFileSecretKey();

        if (accessKey == null) {
            accessKey = cloudProps.getFileUsername();
        }

        if (secretKey == null) {
            secretKey = cloudProps.getFilePassword();
        }

        if (Utils.isNotBlank(accessKey) && Utils.isNotBlank(secretKey)) {
            return createClient(endpoint, regionId, accessKey, secretKey, cloudProps.getProp("file"));
        } else {
            // Use the default provider chain if no credentials are explicitly provided
            return S3Client.builder().build();
        }
    }

    public static S3Client createClient(String endpoint, String regionId, String accessKey, String secretKey, Props props) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(credentials);

        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .region(software.amazon.awssdk.regions.Region.of(regionId));

        if (Utils.isNotEmpty(endpoint)) {
            URI endpointUri = URI.create(endpoint);
            builder.endpointOverride(endpointUri);
        }

        //注入配置
        if (props != null && props.size() > 0) {
            Utils.injectProperties(builder, props);
        }

        return builder.build();
    }

    /**
     * 创建存储桶
     */
    public static boolean createBucket(S3Client client, String bucketName, PolicyType policyType) {
        if (bucketExists(client, bucketName)) {
            return true;
        }

        if (policyType == null) {
            policyType = PolicyType.READ;
        }

        String bucketPolicy = buildBucketPolicy(bucketName, policyType);

        software.amazon.awssdk.services.s3.model.CreateBucketRequest createBucketRequest = software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder()
                .bucket(bucketName)
                .acl(BucketCannedACL.PUBLIC_READ)
                .build();
        client.createBucket(createBucketRequest);

        PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(bucketPolicy)
                .build();
        client.putBucketPolicy(putBucketPolicyRequest);

        return true;
    }

    /**
     * 检查存储桶是否存在
     */
    public static boolean bucketExists(S3Client client, String bucketName) {
        try {
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            client.headBucket(headBucketRequest);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }

    /**
     * 构建策略信息
     */
    private static String buildBucketPolicy(String bucketName, PolicyType policyType) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n\"Statement\": [\n{\n\"Action\": [\n");
        if (policyType == PolicyType.WRITE) {
            builder.append("\"s3:GetBucketLocation\",\n\"s3:ListBucketMultipartUploads\"\n");
        } else if (policyType == PolicyType.READ_WRITE) {
            builder.append("\"s3:GetBucketLocation\",\n\"s3:ListBucket\",\n\"s3:ListBucketMultipartUploads\"\n");
        } else {
            builder.append("\"s3:GetBucketLocation\"\n");
        }
        builder.append("],\n\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("\"\n},\n");
        if (policyType == PolicyType.READ) {
            builder.append("{\n\"Action\": [\n\"s3:ListBucket\"\n],\n\"Effect\": \"Deny\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
            builder.append(bucketName);
            builder.append("\"\n},\n");
        }
        builder.append("{\n\"Action\": ");
        switch (policyType) {
            case WRITE:
                builder.append("[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n");
                break;
            case READ_WRITE:
                builder.append("[\n\"s3:AbortMultipartUpload\",\n\"s3:DeleteObject\",\n\"s3:GetObject\",\n\"s3:ListMultipartUploadParts\",\n\"s3:PutObject\"\n],\n");
                break;
            default:
                builder.append("\"s3:GetObject\",\n");
                break;
        }
        builder.append("\"Effect\": \"Allow\",\n\"Principal\": \"*\",\n\"Resource\": \"arn:aws:s3:::");
        builder.append(bucketName);
        builder.append("/*\"\n}\n],\n\"Version\": \"2012-10-17\"\n}\n");
        return builder.toString();
    }
}
