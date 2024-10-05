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
package org.noear.solon.cloud.extend.qiniu.kodo.service;

import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.handle.Result;
import org.noear.solon.net.http.HttpResponse;
import org.noear.solon.net.http.HttpUtils;

import java.io.IOException;
import java.util.Date;

/**
 * @author noear
 * @since 1.10
 */
public class CloudFileServiceKodoImp implements CloudFileService {

    protected final String bucketDef;
    protected final String regionId;
    protected final String accessKey;
    protected final String secretKey;
    protected final String endpoint;

    protected final Auth auth;
    protected final UploadManager uploadManager;
    protected final BucketManager bucketManager;

    public CloudFileServiceKodoImp(CloudProps cloudProps) {
        this(cloudProps, null);
    }

    public CloudFileServiceKodoImp(CloudProps cloudProps, Region region) {
        bucketDef = cloudProps.getFileBucket();
        regionId  = cloudProps.getFileRegionId();
        accessKey = cloudProps.getFileAccessKey();
        secretKey = cloudProps.getFileSecretKey();
        endpoint = cloudProps.getFileEndpoint();

        auth = Auth.create(accessKey, secretKey);

        //构造一个带指定 Region 对象的配置类
        Configuration cfg = buildConfig(region);

        //...其他参数参考类注释
        uploadManager = new UploadManager(cfg);
        bucketManager = new BucketManager(auth, cfg);
    }

    public Configuration buildConfig(Region region) {
        if (region != null) {
            return new Configuration(region);
        }

        switch (regionId) {
            case "z0":
                return new Configuration(Region.region0());
            case "huadong":
                return new Configuration(Region.huadong());

            case "cn-east-2":
                return new Configuration(Region.regionCnEast2());
            case "zhejiang2":
                return new Configuration(Region.huadongZheJiang2());

            case "z1":
                return new Configuration(Region.region1());
            case "huabei":
                return new Configuration(Region.huabei());

            case "z2":
                return new Configuration(Region.region2());
            case "huanan":
                return new Configuration(Region.huanan());

            case "na0":
                return new Configuration(Region.regionNa0());
            case "beimei":
                return new Configuration(Region.beimei());

            case "as0":
            case "xinjiapo":
                return new Configuration(Region.xinjiapo());

            case "fog-cn-east-1":
                return new Configuration(Region.regionFogCnEast1());

            default:
                return new Configuration();
        }
    }


    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        String baseUrl = buildUrl(key);
        String downUrl = auth.privateDownloadUrl(baseUrl);

        try {
            int code = HttpUtils.http(downUrl).execAsCode("HEAD");

            return code == 200;
        } catch (IOException e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Date expiration) throws CloudFileException, UnsupportedOperationException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        long seconds = (expiration.getTime() - System.currentTimeMillis()) / 1000L;

        String baseUrl = buildUrl(key);
        String downUrl = auth.privateDownloadUrl(baseUrl, seconds);

        return downUrl;
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        String baseUrl = buildUrl(key);
        String downUrl = auth.privateDownloadUrl(baseUrl);

        try {
            HttpResponse resp = HttpUtils.http(downUrl).exec("GET");

            return new Media(resp.body(), resp.contentType(), resp.contentLength());
        } catch (IOException e) {
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

        String uploadToken = auth.uploadToken(bucket);

        try {
            Response resp = uploadManager.put(media.body(), key, uploadToken, new StringMap(), streamMime);
            return Result.succeed(resp.bodyString());
        } catch (QiniuException e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            Response resp = bucketManager.delete(bucket, key);
            return Result.succeed(resp.bodyString());
        } catch (QiniuException e) {
            return Result.failure(e.error());
        }
    }

    private String buildUrl(String key) {
        if (endpoint.contains("://")) {
            return endpoint + "/" + key;
        } else {
            return "https://" + endpoint + "/" + key;
        }
    }
}
