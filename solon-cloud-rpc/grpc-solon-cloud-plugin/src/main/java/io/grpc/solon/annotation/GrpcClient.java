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
package io.grpc.solon.annotation;

import io.grpc.netty.NegotiationType;
import org.noear.solon.annotation.Alias;

import java.lang.annotation.*;

/**
 * @author noear
 * @since 1.9
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClient {
    /**
     * 应用名
     */
    @Alias("name")
    String value() default "";

    /**
     * 应用名
     */
    @Alias("value")
    String name() default "";

    /**
     * 应用分组
     */
    String group() default "";

    /**
     * 协商类型
     */
    NegotiationType negotiationType() default NegotiationType.PLAINTEXT;
}
