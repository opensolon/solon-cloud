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
package org.noear.solon.cloud.service;

import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.core.handle.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;

/**
 * 云端文件服务（分布式文件服务服务）
 *
 * @author noear
 * @since 1.3
 */
public interface CloudFileService {

    /**
     * 是否存在
     *
     * @param bucket 存储桶
     * @param key    存储键
     */
    boolean exists(String bucket, String key) throws CloudFileException;

    /**
     * 是否存在
     *
     * @param key 存储键
     */
    default boolean exists(String key) throws CloudFileException {
        return exists(null, key);
    }

    /**
     * 获取文件临时地址
     *
     * @param bucket 存储桶
     * @param key    存储键
     */
    String getTempUrl(String bucket, String key, Duration duration) throws CloudFileException, UnsupportedOperationException;


    /**
     * 获取文件临时地址
     *
     * @param key 存储键
     */
    default String getTempUrl(String key, Duration duration) throws CloudFileException, UnsupportedOperationException {
        return getTempUrl(null, key, duration);
    }

    /**
     * 获取文件临时地址
     *
     * @param bucket 存储桶
     * @param key    存储键
     */
    default String getTempUrl(String bucket, String key, Date expiration) throws CloudFileException, UnsupportedOperationException {
        return getTempUrl(bucket, key, Duration.between(Instant.now(), expiration.toInstant()));
    }


    /**
     * 获取文件临时地址
     *
     * @param key 存储键
     */
    default String getTempUrl(String key, Date expiration) throws CloudFileException, UnsupportedOperationException {
        return getTempUrl(null, key, expiration);
    }


    /**
     * 获取文件
     *
     * @param bucket 存储桶
     * @param key    存储键
     */
    Media get(String bucket, String key) throws CloudFileException;

    /**
     * 获取文件
     *
     * @param key 存储键
     */
    default Media get(String key) throws CloudFileException {
        return get(null, key);
    }

    /**
     * 推入文件
     *
     * @param bucket 存储桶
     * @param key    存储键
     * @param media  媒体
     */
    Result put(String bucket, String key, Media media) throws CloudFileException;

    /**
     * 推入文件
     *
     * @param key   存储键
     * @param media 媒体
     */
    default Result put(String key, Media media) throws CloudFileException {
        return put(null, key, media);
    }

    /**
     * 删除文件
     *
     * @param bucket 存储桶
     * @param key    存储键
     */
    Result delete(String bucket, String key) throws CloudFileException;

    /**
     * 删除文件
     *
     * @param key 存储键
     */
    default Result delete(String key) {
        return delete(null, key);
    }

    /**
     * 批量删除文件
     *
     * @param bucket 存储桶
     * @param keys   存储键集合
     */
    Result deleteBatch(String bucket, Collection<String> keys) throws CloudFileException;

    /**
     * 批量删除文件
     *
     * @param keys 存储键集合
     */
    default Result deleteBatch(Collection<String> keys) throws CloudFileException {
        return deleteBatch(null, keys);
    }
}