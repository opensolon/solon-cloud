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
package org.noear.solon.cloud.extend.local.service;

import org.noear.solon.Utils;
import org.noear.solon.cloud.exception.CloudFileException;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.cloud.service.CloudFileService;
import org.noear.solon.core.handle.Result;
import org.noear.solon.core.util.IoUtil;

import java.io.*;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author noear
 * @since 1.12
 */
public class CloudFileServiceLocalImpl implements CloudFileService {
    private File root;
    private final ReentrantLock SYNC_LOCK = new ReentrantLock();

    public CloudFileServiceLocalImpl(String rootDir) {
        this.root = new File(rootDir, "file");

        if (root.exists() == false) {
            root.mkdirs();
        }
    }

    @Override
    public boolean exists(String bucket, String key) throws CloudFileException {
        try {
            return getFile(bucket, key).exists();
        } catch (Throwable e) {
            throw new CloudFileException(e);
        }
    }

    @Override
    public String getTempUrl(String bucket, String key, Date expiration) throws CloudFileException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Media get(String bucket, String key) throws CloudFileException {
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
