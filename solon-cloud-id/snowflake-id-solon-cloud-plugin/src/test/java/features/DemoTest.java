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
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.cloud.CloudClient;
import org.noear.solon.cloud.extend.snowflake.impl.SnowflakeId;
import org.noear.solon.test.SolonJUnit5Extension;
import org.noear.solon.test.SolonTest;

/**
 * @author noear 2021/10/9 created
 */
@SolonTest
public class DemoTest {
    @Test
    public void test() {
        SnowflakeId snowflakeId = new SnowflakeId(1, 1);
        SnowflakeId idWorker = new SnowflakeId(1, 1);

        long id10 = snowflakeId.nextId();
        long id11 = idWorker.nextId();

        System.out.println(id10);
        System.out.println(id11);

        assert id10 == id11;

        long id20 = snowflakeId.nextId();
        long id21 = idWorker.nextId();

        System.out.println(id20);
        System.out.println(id21);

        assert id20 == id21;
        assert id20 != id10;

        long id30 = snowflakeId.nextId();
        long id31 = idWorker.nextId();

        System.out.println(id30);
        System.out.println(id31);

        assert id30 == id31;
        assert id30 != id20;
    }

    @Test
    public void test2() {
        long id10 = CloudClient.id().generate();
        long id11 = CloudClient.id().generate();
        System.out.println(id10);
        assert id10 != id11;

        System.out.println(CloudClient.idService("demo", "demoapi").generate());
    }
}
