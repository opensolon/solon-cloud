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
package libs._test_proxy;

import org.junit.jupiter.api.Test;
import org.noear.solon.test.HttpTester;
import org.noear.solon.test.SolonTest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author noear 2022/3/22 created
 */
@SolonTest(App.class)
public class HttpUploadTest extends HttpTester {
    @Test
    public void upload() throws IOException {
        StringBuilder buf = new StringBuilder();
        while (buf.length() < 1024 *1024){
            buf.append("test:1234567890abcdefg;");
        }

        InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f1")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .post().contains("成功：装修-水电-视频.mp4");
    }

    @Test
    public void upload2() throws IOException {
        StringBuilder buf = new StringBuilder();
        while (buf.length() < 1024 *1024){
            buf.append("test:1234567890abcdefg;");
        }

        InputStream inputStream = new ByteArrayInputStream(buf.toString().getBytes(StandardCharsets.UTF_8));
        InputStream inputStream2 = new ByteArrayInputStream(buf.toString().getBytes(StandardCharsets.UTF_8));

        String rst = path("/demo3/upload/f1")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .data("file2", "测试2.mp4", inputStream2, "video/mp4")
                .post();

        assert rst.contains("成功：装修-水电-视频.mp4");
        assert rst.contains("测试2.mp4");
    }

//    @Test
//    public void upload_empty() throws IOException {
//        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
//
//        assert path("/demo3/upload/f11")
//                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
//                .data("userName", "noear")
//                .post().contains("我没接数据：）");
//
//
//        inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
//
//        assert path("/demo3/upload/f11_2")
//                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
//                .data("userName", "noear")
//                .post().contains("noear");
//    }

    @Test
    public void upload_param() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f12")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .data("userName", "noear")
                .post().startsWith("noear");
    }

    @Test
    public void upload_param_1() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f12_1")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .data("userName", "noear")
                .post().startsWith("noear");
    }

    @Test
    public void upload_param_2() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f13")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .data("userName", "noear")
                .post().startsWith("noear");
    }

    @Test
    public void upload_param_2_2() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f13_2")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .data("userName", "noear")
                .post().startsWith("noear");
    }

    @Test
    public void upload_param_f3() throws IOException {
        InputStream inputStream = new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

        assert path("/demo3/upload/f3")
                .data("file", "装修-水电-视频.mp4", inputStream, "video/mp4")
                .post().equals("1");
    }
}
