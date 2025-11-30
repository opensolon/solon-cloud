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
package org.noear.solon.cloud.telemetry.integration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.telemetry.slf4j.TracingMDC;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import java.util.HashSet;
import java.util.Set;

/**
 * Solon Tracing 过滤器适配
 *
 * @author noear
 * @since 3.7
 */
public class SolonFilterTracing implements Filter {
    private Set<String> excludePaths = new HashSet<>();

    private Tracer tracer;
    private TextMapPropagator propagator;

    public SolonFilterTracing(String excluded) {
        //排除支持
        if (Utils.isNotEmpty(excluded)) {
            for (String path : excluded.split(",")) {
                path = path.trim();

                if (path.length() > 0) {
                    if (path.startsWith("/")) {
                        excludePaths.add(path);
                    } else {
                        excludePaths.add("/" + path);
                    }
                }
            }
        }

        // 获取 Tracer 和 Propagator
        Solon.context().getBeanAsync(OpenTelemetry.class, bean -> {
            tracer = bean.getTracer(SolonFilterTracing.class.getName());
            propagator = bean.getPropagators().getTextMapPropagator();
        });
    }

    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
        if (tracer == null || propagator == null || excludePaths.contains(ctx.pathNew())) {
            //没有跟踪器，或者排除
            chain.doFilter(ctx);
        } else {
            Span span = buildSpan(ctx);

            try (Scope scope = span.makeCurrent()) {
                TracingMDC.inject(span);

                chain.doFilter(ctx);
            } catch (Throwable e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, "Request failed");
                throw e;
            } finally {
                TracingMDC.removeSpanId();
                TracingMDC.removeTraceId();
                span.end();
            }
        }
    }

    public Span buildSpan(Context ctx) {
        //获取上下文
        io.opentelemetry.context.Context parentContext = propagator.extract(io.opentelemetry.context.Context.current(), ctx, new SolonHeaderGetter());


        //实例化构建器
        StringBuilder operationName = new StringBuilder();
        operationName.append(ctx.pathNew()).append(" (").append(ctx.uri().getScheme()).append(" ").append(ctx.method()).append(')');

        SpanBuilder spanBuilder = tracer.spanBuilder(operationName.toString());

        //添加种类标志
        spanBuilder.setParent(parentContext);
        spanBuilder.setSpanKind(SpanKind.SERVER);
        spanBuilder.setAttribute("req.url", ctx.url());
        spanBuilder.setAttribute("req.method", ctx.method());

        Span span = spanBuilder.startSpan();

        //开始
        return span;
    }

    public static class SolonHeaderGetter implements TextMapGetter<Context> {
        @Override
        public Iterable<String> keys(Context ctx) {
            return ctx.headerNames();
        }

        @Override
        public String get(Context ctx, String key) {
            return ctx.header(key);
        }
    }

}