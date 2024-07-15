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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudFileException;

import java.net.URI;
import java.util.Properties;

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
    public static AmazonS3 createClient(CloudProps cloudProps) {
        String endpoint = cloudProps.getFileEndpoint();
        String regionId = cloudProps.getFileRegionId();

        String accessKey = cloudProps.getFileAccessKey();
        String secretKey = cloudProps.getFileSecretKey();

//        if (Utils.isEmpty(regionId) && Utils.isEmpty(endpoint)) {
//            throw new CloudFileException("The 'regionId' and 'endpoint' configuration must have one");
//        }
        if (Utils.isNotBlank(accessKey) && Utils.isNotBlank(secretKey)) {
            return createClient(endpoint, regionId, accessKey, secretKey, cloudProps.getProp("file"));
        }else {
            //使用 AWS 默认凭证提供程序
            return AmazonS3ClientBuilder.defaultClient();
        }
    }

    public static AmazonS3 createClient(String endpoint, String regionId, String accessKey, String secretKey, Properties props) {
        //初始化client
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        ClientConfiguration clientConfig = new ClientConfiguration();

        if (Utils.isEmpty(endpoint)) {
            clientConfig.setProtocol(Protocol.HTTPS);

            return AmazonS3ClientBuilder.standard()
                    .withRegion(regionId)
                    .withClientConfiguration(clientConfig)
                    .withCredentials(credentialsProvider)
                    .build();
        } else {
            final URI endpointUri;
            if (endpoint.contains("://")) {
                endpointUri = URI.create(endpoint);
                endpoint = endpointUri.getHost();
            } else {
                endpointUri = URI.create("https://" + endpoint);
            }

            if ("http".equals(endpointUri.getScheme())) {
                clientConfig.setProtocol(Protocol.HTTP);
            } else {
                clientConfig.setProtocol(Protocol.HTTPS);
            }

            AwsClientBuilder.EndpointConfiguration endpointConfig = new AwsClientBuilder
                    .EndpointConfiguration(endpoint, regionId);

            //开始构建
            AmazonS3ClientBuilder builder = AmazonS3Client.builder()
                    .withEndpointConfiguration(endpointConfig)
                    .withClientConfiguration(clientConfig)
                    .withCredentials(credentialsProvider);

            //注入配置
            if (props != null && props.size() > 0) {
                Utils.injectProperties(builder, props);
            }

            return builder.build();
        }
    }

    /**
     * 创建存储桶
     */
    public static boolean createBucket(AmazonS3 client, String bucketName, PolicyType policyType) {
        if (client.doesBucketExistV2(bucketName)) {
            return true;
        }

        if (policyType == null) {
            policyType = PolicyType.READ;
        }

        String bucketPolicy = buildBucketPolicy(bucketName, policyType);

        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        createBucketRequest.setCannedAcl(CannedAccessControlList.PublicRead);
        client.createBucket(createBucketRequest);
        client.setBucketPolicy(bucketName, bucketPolicy);

        return true;
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
