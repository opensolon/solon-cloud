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
package features;

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.extend.quartz.service.Cron7X;

import java.time.ZoneOffset;

/**
 * @author noear 2024/4/16 created
 */
public class Cron7XTest {
    @Test
    public void cron7x1() {
        Cron7X cron7x = Cron7X.parse("12ms");
        assert cron7x.getInterval() == 12L;
    }

    @Test
    public void cron7x2() {
        Cron7X cron7x = Cron7X.parse("* * * * * ?+08");
        assert cron7x.getZone() == ZoneOffset.ofHours(8);
    }
}
