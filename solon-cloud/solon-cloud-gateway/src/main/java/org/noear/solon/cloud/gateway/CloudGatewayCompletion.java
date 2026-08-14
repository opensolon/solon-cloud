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
package org.noear.solon.cloud.gateway;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.exchange.ExBody;
import org.noear.solon.cloud.gateway.exchange.ExConstants;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfBuffer;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfStream;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.core.util.KeyValues;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 响应式完成器
 *
 * @author noear
 * @since 2.9
 */
public class CloudGatewayCompletion implements Subscriber<Void> {
    static final Logger log = LoggerFactory.getLogger(CloudGatewayCompletion.class);

    private final ExContext ctx;
    private final HttpServerRequest rawRequest;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    public CloudGatewayCompletion(ExContext ctx, HttpServerRequest rawRequest) {
        this.ctx = ctx;
        this.rawRequest = rawRequest;
    }


    @Override
    public void onSubscribe(Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Void unused) {
        //不管
    }

    @Override
    public void onError(Throwable err) {
        try {
            if (err instanceof StatusException) {
                StatusException status = (StatusException) err;
                ctx.newResponse().status(status.getCode());

                if (status.getCode() == 404) {
                    return;
                }
            } else {
                ctx.newResponse().status(500);
            }

            log.warn(err.getMessage(), err);
        } finally {
            postComplete();
        }

    }

    @Override
    public void onComplete() {
        postComplete();
    }

    /**
     * 提交异步完成
     */
    public void postComplete() {
        if (completed.compareAndSet(false, true) == false) {
            return;
        }

        try {
            HttpServerResponse rawResponse = rawRequest.response();

            if (rawResponse.headWritten() == false) {
                rawResponse.setStatusCode(ctx.newResponse().getStatus());

                if (Utils.isNotEmpty(ctx.newResponse().getReason())) {
                    rawResponse.setStatusMessage(ctx.newResponse().getReason());
                }

                for (KeyValues<String> kv : ctx.newResponse().getHeaders()) {
                    rawResponse.putHeader(kv.getKey(), kv.getValues());
                }
            }

            if (rawResponse.ended() == false) {
                if (ctx.newResponse().getBody() != null) {
                    ExBody exBody = ctx.newResponse().getBody();
                    if (exBody instanceof ExBodyOfStream) {
                        //也说明没改过
                        rawResponse.send(((ExBodyOfStream) exBody).getStream());
                    } else if (exBody instanceof ExBodyOfBuffer) {
                        Buffer buffer = ((ExBodyOfBuffer) exBody).getBuffer();
                        rawResponse.putHeader(ExConstants.Content_Length, String.valueOf(buffer.length()));
                        rawResponse.send(buffer);
                    } else {
                        rawResponse.end();
                    }
                } else {
                    rawResponse.end();
                }
            }
        } catch (Throwable err) {
            log.warn(err.getMessage(), err);
        }
    }
}