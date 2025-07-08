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
package org.noear.solon.cloud.extend.aliyun.oss.service;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.Protocol;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.handle.Result;
import org.noear.solon.net.http.HttpResponse;
import org.noear.solon.net.http.HttpUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.*;

/**
 * 云端文件服务（aliyun oss）
 *
 * @author noear
 * @since 1.3
 */
public class CloudFileServiceOssImpl implements CloudFileService {
    private static final String CHARSET_UTF8 = "utf8";
    private static final String ALGORITHM = "HmacSHA1";
    public static OSS client;

    private final String bucketDef;

    private final String accessKey;
    private final String secretKey;
    private final String endpoint;


    public CloudFileServiceOssImpl(CloudProps cloudProps) {
        this(
                cloudProps.getFileEndpoint(),
                cloudProps.getFileBucket(),
                cloudProps.getFileAccessKey(),
                cloudProps.getFileSecretKey()
        );
    }


    public CloudFileServiceOssImpl(String endpoint, String bucket, String accessKey, String secretKey) {
        if(Utils.isEmpty(endpoint)){
            throw new IllegalArgumentException("The endpoint configuration is missing");
        }

        this.endpoint = endpoint;

        this.bucketDef = bucket;

        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public OSS getClient() {
        if (client != null) {
            return client;
        }
        synchronized (this) {
            if (client != null) {
                return client;
            }
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSupportCname(true);
            clientBuilderConfiguration.setProtocol(Protocol.HTTPS);
            clientBuilderConfiguration.setMaxConnections(200);
            clientBuilderConfiguration.setConnectionTimeout(5000);
            clientBuilderConfiguration.setMaxErrorRetry(3);
            OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey, clientBuilderConfiguration);
            CloudFileServiceOssImpl.client = ossClient;
            return ossClient;
        }
    }


    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            String date = Datetime.Now().toGmtString();

            String objPath = "/" + bucket + "/" + key;
            String url = buildUrl(bucket, key);

            String Signature = (hmacSha1(buildSignData("HEAD", date, objPath, null), secretKey));

            String Authorization = "OSS " + accessKey + ":" + Signature;

            Map<String, String> head = new HashMap<String, String>();
            head.put("Date", date);
            head.put("Authorization", Authorization);

            int code = HttpUtils.http(url)
                    .header("Date", date)
                    .header("Authorization", Authorization)
                    .execAsCode("HEAD");

            return code == 200;
        } catch (Exception ex) {
            throw new CloudFileException(ex);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Duration duration) throws CloudFileException, UnsupportedOperationException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }
        Date expiration = new Date(System.currentTimeMillis() + duration.getSeconds() * 1000);
        return getClient().generatePresignedUrl(bucket, key, expiration).toString();
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            String date = Datetime.Now().toGmtString();

            String objPath = "/" + bucket + "/" + key;
            String url = buildUrl(bucket, key);

            String Signature = (hmacSha1(buildSignData("GET", date, objPath, null), secretKey));

            String Authorization = "OSS " + accessKey + ":" + Signature;

            Map<String, String> head = new HashMap<String, String>();
            head.put("Date", date);
            head.put("Authorization", Authorization);

            HttpResponse resp = HttpUtils.http(url)
                    .header("Date", date)
                    .header("Authorization", Authorization)
                    .exec("GET");

            return new Media(resp.body(), resp.contentType(), resp.contentLength());
        } catch (Exception ex) {
            throw new CloudFileException(ex);
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
            String date = Datetime.Now().toGmtString();

            String objPath = "/" + bucket + "/" + key;
            String url = buildUrl(bucket, key);

            String Signature = (hmacSha1(buildSignData("PUT", date, objPath, streamMime), secretKey));
            String Authorization = "OSS " + accessKey + ":" + Signature;


            String tmp = HttpUtils.http(url)
                    .header("Date", date)
                    .header("Authorization", Authorization)
                    .bodyRaw(media.body(), streamMime)
                    .put();

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

        try {
            String date = Datetime.Now().toGmtString();

            String objPath = "/" + bucket + "/" + key;
            String url = buildUrl(bucket, key);

            String Signature = (hmacSha1(buildSignData("DELETE", date, objPath, null), secretKey));

            String Authorization = "OSS " + accessKey + ":" + Signature;

            String tmp = HttpUtils.http(url)
                    .header("Date", date)
                    .header("Authorization", Authorization)
                    .delete();

            return Result.succeed(tmp);
        } catch (Exception ex) {
            throw new CloudFileException(ex);
        }
    }

    @Override
    public Result deleteBatch(String bucket, Collection<String> keys) throws CloudFileException {
        for (String key : keys) {
            delete(bucket, key);
        }

        return Result.succeed();
    }

    private String buildUrl(String bucket, String key) {
        if (endpoint.startsWith(bucket)) {
            return "https://" + endpoint + "/" + key;
        } else {
            return "https://" + bucket + "." + endpoint + "/" + key;
        }
    }

    private String hmacSha1(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(CHARSET_UTF8));

            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildSignData(String method, String date, String objPath, String contentType) {
        if (contentType == null) {
            return method + "\n\n\n"
                    + date + "\n"
                    + objPath;
        } else {
            return method + "\n\n"
                    + contentType + "\n"
                    + date + "\n"
                    + objPath;
        }
    }
}
