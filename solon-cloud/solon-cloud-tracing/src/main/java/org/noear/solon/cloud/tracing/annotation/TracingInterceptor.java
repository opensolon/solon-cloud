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
package org.noear.solon.cloud.tracing.annotation;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.cloud.tracing.slf4j.TracingMDC;
import org.noear.solon.core.aspect.Interceptor;
import org.noear.solon.core.aspect.Invocation;
import org.noear.solon.core.util.TmplUtil;

/**
 * @author noear
 * @since 1.7
 */
public class TracingInterceptor implements Interceptor {
    private Tracer tracer;

    public TracingInterceptor() {
        Solon.context().getBeanAsync(Tracer.class, bean -> {
            tracer = bean;
        });
    }


    @Override
    public Object doIntercept(Invocation inv) throws Throwable {
        if (tracer == null) {
            return inv.invoke();
        }


        Tracing anno = inv.getMethodAnnotation(Tracing.class);

        if (anno == null) {
            //支持注解在类上
            anno = inv.getTargetAnnotation(Tracing.class);
        }

        if (anno == null) {
            return inv.invoke();
        } else {
            Span span = buildSpan(inv, anno);

            try (Scope scope = tracer.activateSpan(span)) {
                TracingMDC.inject(span);

                return inv.invoke();
            } catch (Throwable e) {
                span.log(Utils.throwableToString(e));
                throw e;
            } finally {
                TracingMDC.removeSpanId();
                span.finish();
            }
        }
    }

    public Span buildSpan(Invocation inv, Tracing anno) {
        String spanName = Utils.annoAlias(anno.value(), anno.name());
        if (Utils.isEmpty(spanName)) {
            spanName = inv.getTargetClz().getSimpleName()
                    + "::"
                    + inv.method().getMethod().getName();
        }

        //实例化构建器
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(spanName);

        spanBuilder.withTag("clz.fullname", inv.getTargetClz().getName());

        //添加标志
        String tags = TmplUtil.parse(anno.tags(), inv);
        if (Utils.isNotEmpty(tags)) {
            for (String tag : tags.split(",")) {
                String[] kv = tag.split("=");
                spanBuilder.withTag(kv[0], kv[1]);
            }
        }

        Span span = spanBuilder.start();


        //开始
        return span;
    }
}
