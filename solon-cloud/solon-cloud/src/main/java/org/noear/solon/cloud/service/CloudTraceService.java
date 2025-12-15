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
package org.noear.solon.cloud.service;

import org.noear.solon.core.util.CallableTx;
import org.noear.solon.core.util.RunnableTx;

/**
 * 云端链路跟踪服务
 *
 * @author noear
 * @since 1.2
 */
public interface CloudTraceService {
    /**
     * TraceId 头名称
     */
    String HEADER_TRACE_ID_NAME();

    /**
     * FromId 头名称
     *
     */
    String HEADER_FROM_ID_NAME();

    /**
     * @since 3.7.4
     */
    <X extends Throwable> void with(String traceId, RunnableTx<X> runnable) throws X;

    /**
     * @since 3.7.4
     */
    <R, X extends Throwable> R with(String traceId, CallableTx<R, X> callable) throws X;


    /**
     * @since 3.7.4
     */
    <X extends Throwable> void with(RunnableTx<X> runnable) throws X;

    /**
     * @since 3.7.4
     */
    <R, X extends Throwable> R with(CallableTx<R, X> callable) throws X;

    //----------------------------

    /**
     * 设置当前线程的跟踪标识
     *
     * @deprecated 3.7.4 {@link #with(String, RunnableTx)} {@link #with(String, CallableTx)}
     */
    @Deprecated
    void setLocalTraceId(String traceId);

    /**
     * 获取跟踪标识
     */
    String getTraceId();

    /**
     * 获取来源标识（service@address:port）
     *
     */
    String getFromId();
}
