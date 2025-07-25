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

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.model.Media;
import org.noear.solon.core.handle.Result;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

/**
 * @author noear 2021/10/18 created
 */
@SolonTest(DemoApp.class)
public class DemoTest {
    static Logger log = LoggerFactory.getLogger(DemoTest.class);

    final String contentBody = "test s3 rest api";
    final Media media = new Media(contentBody);
    final String key = "test0/test.txt";

    @Test
    public void test0() throws IOException {
        long start = System.currentTimeMillis();

        //写入
        Result result = CloudClient.file().put(key, media);
        log.info("put result: {}", result);
        assert result.getCode() == 200;

        //获取
        Media getMedia = CloudClient.file().get(key);
        String getBodyString = getMedia.bodyAsString(true);
        log.info("getMedia size:{}, body: {}", getMedia.contentSize(), getBodyString);
        assert getBodyString.equals(contentBody);

        //删除
        result = CloudClient.file().delete(key);
        log.debug("delete result: {}", result);
        assert result.getCode() == 200;

        System.out.println("times: " + (System.currentTimeMillis() - start));
    }

    @Test
    public void testGetTempUrl() {
        CloudClient.file().put(key, media);
        String url = CloudClient.file().getTempUrl(key, Duration.ofMinutes(10));
        System.out.println(url);
    }
}
