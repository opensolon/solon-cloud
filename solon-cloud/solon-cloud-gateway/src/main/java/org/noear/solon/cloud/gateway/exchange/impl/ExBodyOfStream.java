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
package org.noear.solon.cloud.gateway.exchange.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import org.noear.solon.cloud.gateway.exchange.ExBody;

/**
 * @author noear
 * @since 2.9
 */
public class ExBodyOfStream implements ExBody {
    private ReadStream<Buffer> stream;
    public ExBodyOfStream(ReadStream<Buffer> stream){
        this.stream = stream;
    }

    public ReadStream<Buffer> getStream() {
        return stream;
    }
}
