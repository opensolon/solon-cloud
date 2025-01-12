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
package demo;

import org.noear.solon.annotation.*;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.DownloadedFile;
import org.noear.solon.core.handle.Result;
import org.noear.solon.core.handle.UploadedFile;

import java.io.IOException;

/**
 * @author noear 2022/12/17 created
 */
//@Mapping("file")
//@Controller
public class FileController {

    /**
     * 文件下载
     *
     * @param ctx    请求上下文
     * @param bucket 存储桶
     */
    //@Get
    //@Mapping("{bucket}/**")
    public DownloadedFile get(Context ctx, String bucket) throws IOException {
        String pathPrefix = "/file/" + bucket + "/";
        String fileName = ctx.path().substring(pathPrefix.length());

        Media media = CloudClient.file().get(bucket, fileName);

        //把数据读出来，不然长度可能获取不到

        return new DownloadedFile(media.contentType(), media.contentSize(), media.body(), fileName);
    }

    /**
     * 文件上传
     *
     * @param ctx    请求上下文
     * @param bucket 存储桶
     */
    //@Post
    //@Mapping("{bucket}/**")
    public Result post(Context ctx, String bucket, UploadedFile file) {
        String pathPrefix = "/file/" + bucket + "/";
        String fileName = ctx.path().substring(pathPrefix.length());

        Media media = new Media(file.getContent(), file.getContentType());
        return CloudClient.file().put(bucket, fileName, media);
    }

    /**
     * 文件删除
     *
     * @param ctx    请求上下文
     * @param bucket 存储桶
     */
    //@Delete
    //@Mapping("{bucket}/**")
    public Result delete(Context ctx, String bucket) {
        String pathPrefix = "/file/" + bucket + "/";
        String fileName = ctx.path().substring(pathPrefix.length());

        return CloudClient.file().delete(bucket, fileName);
    }
}
