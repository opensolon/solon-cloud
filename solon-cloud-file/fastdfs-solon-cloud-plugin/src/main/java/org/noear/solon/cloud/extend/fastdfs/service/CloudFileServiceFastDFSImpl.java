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
package org.noear.solon.cloud.extend.fastdfs.service;

import org.csource.fastdfs.*;
import org.noear.solon.Utils;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.handle.Result;

import java.time.Duration;
import java.util.Collection;
import java.util.Properties;

/**
 * 云文件服务 FastDFS
 *
 * @author liaocp
 * @since 1.12
 */
public class CloudFileServiceFastDFSImpl implements CloudFileService {
    private static final String DEF_CONFIG_FILE = "META-INF/solon_def/fastdfs_def.properties";

    private final String bucketDef;

    private final StorageClient client;

    /**
     * 获取真实客户端
     */
    public StorageClient getClient() {
        return client;
    }

    public CloudFileServiceFastDFSImpl(AppContext appContext, CloudProps cloudProps) {
        bucketDef = cloudProps.getFileBucket();

        //构建属性
        Properties props = Utils.loadProperties(DEF_CONFIG_FILE);
        Properties propsTmp = appContext.cfg().getProp("fastdfs");
        propsTmp.forEach((key, val) -> {
            props.put("fastdfs." + key, val);
        });

        //构建 servers
        String servers = cloudProps.getFileEndpoint();
        if (Utils.isNotEmpty(servers)) {
            props.setProperty("fastdfs.tracker_servers", servers);
        }

        //构建 secret_key
        String secret_key = cloudProps.getFileSecretKey();
        if (Utils.isNotEmpty(secret_key)) {
            props.setProperty("fastdfs.http_secret_key", secret_key);
        }


        try {
            ClientGlobal.initByProperties(props);

            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer = trackerClient.getTrackerServer();
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            client = new StorageClient(trackerServer, storageServer);
        } catch (Exception e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            FileInfo fileInfo = client.get_file_info(bucket, key);


            return fileInfo != null && fileInfo.getFileSize() > 0;
        } catch (Exception e) {
            throw new CloudFileException("Cloud file get failure: " + key, e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Duration duration) throws CloudFileException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            byte[] resultByte = client.download_file(bucket, key);
            return new Media(resultByte);
        } catch (Exception e) {
            throw new CloudFileException("Cloud file get failure: " + key, e);
        }
    }

    @Override
    public Result<Object> put(String bucket, String key, Media media) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        //处理后缀名
        String sufName = null;
        int sufIdx = key.lastIndexOf(".");

        if (sufIdx > 0) {
            sufName = key.substring(sufIdx + 1);
        }

        if (Utils.isEmpty(sufName)) {
            throw new CloudFileException("the file extension must not be empty: " + key);
        }

        String[] result;
        try {
            result = client.upload_file(bucket, media.bodyAsBytes(), sufName, null);
        } catch (Exception e) {
            throw new CloudFileException("Cloud file put failure: " + key, e);
        }

        if (result == null) {
            throw new CloudFileException("Cloud file put failure code[" + client.getErrorCode() + "]: " + key);
        } else {
            return Result.succeed(result);
        }
    }

    @Override
    public Result<Object> delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            int tmp = client.delete_file(bucket, key);

            return Result.succeed(tmp);
        } catch (Exception e) {
            throw new CloudFileException("Cloud file delete failure: " + key, e);
        }
    }

    @Override
    public Result deleteBatch(String bucket, Collection<String> keys) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        for (String key : keys) {
            try {
                client.delete_file(bucket, key);
            } catch (Exception e) {
                throw new CloudFileException("Cloud file delete failure: " + key, e);
            }
        }

        return Result.succeed();
    }
}