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
package org.noear.solon.cloud.extend.file.s3.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.Props;
import org.noear.solon.core.handle.Result;
import org.noear.solon.core.util.IoUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CloudFileService 的本地实现
 *
 * @author 等風來再離開
 * @author noear
 * @since 1.11
 */
public class CloudFileServiceOfLocalImpl implements CloudFileService {
    private final String bucketDef;
    private final File root;
    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public CloudFileServiceOfLocalImpl(String bucketDef, Props props) {
        String endpoint = props.getProperty("endpoint");

        this.bucketDef = bucketDef;
        this.root = new File(endpoint);

        if (root.exists() == false) {
            root.mkdirs();
        }
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            return getFile(bucket, key).exists();
        } catch (Throwable e) {
            throw new CloudFileException(e);
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
            File file = getFile(bucket, key);

            if (file.exists()) {
                String contentType = Utils.mime(file.getName());
                return new Media(new FileInputStream(file), contentType);
            } else {
                return null;
            }
        } catch (Throwable e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result put(String bucket, String key, Media media) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            File file = getFile(bucket, key);
            if (file.exists() == false) {
                file.createNewFile();
            }

            try (OutputStream stream = new FileOutputStream(file, false)) {
                IoUtil.transferTo(media.body(), stream);
            }

            return Result.succeed(file.getAbsolutePath());
        } catch (Throwable e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result delete(String bucket, String key) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            File file = getFile(bucket, key);
            if (file.exists()) {
                if (file.delete() == false) {
                    return Result.failure();
                }
            }

            return Result.succeed(file.getAbsolutePath());
        } catch (Throwable e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public Result deleteBatch(String bucket, Collection<String> keys) throws CloudFileException {
        if (Utils.isEmpty(bucket)) {
            bucket = bucketDef;
        }

        try {
            for (String key : keys) {
                File file = getFile(bucket, key);
                if (file.exists()) {
                    if (file.delete() == false) {
                        return Result.failure();
                    }
                }
            }

            return Result.succeed();
        } catch (Throwable e) {
            throw new CloudFileException(e);
        }
    }

    private File getFile(String bucket, String key) {
        if (Utils.isEmpty(bucket)) {
            bucket = "DEFAULT_BUCKET";
        }

        File dir = new File(root, bucket);

        SYNC_LOCK.lock();

        try {
            if (dir.exists() == false) {
                if (dir.exists() == false) {
                    dir.mkdirs();
                }
            }

            int last = key.lastIndexOf('/');
            if (last > 0) {
                String dir2Str = key.substring(0, last);
                File dir2Tmp = new File(dir, dir2Str);
                if (dir2Tmp.exists() == false) {
                    if (dir2Tmp.exists() == false) {
                        dir2Tmp.mkdirs();
                    }
                }
            }
        } finally {
            SYNC_LOCK.unlock();
        }

        File file = new File(dir, key);

        return file;
    }
}
