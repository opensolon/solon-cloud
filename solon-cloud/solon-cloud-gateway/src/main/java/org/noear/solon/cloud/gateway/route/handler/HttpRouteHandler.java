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
package org.noear.solon.cloud.gateway.route.handler;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.exchange.ExBody;
import org.noear.solon.cloud.gateway.exchange.ExConstants;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExHeaderUtils;
import org.noear.solon.cloud.gateway.exchange.XForwardedHeaders;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfBuffer;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfStream;
import org.noear.solon.cloud.gateway.properties.HttpClientProperties;
import org.noear.solon.cloud.gateway.properties.HttpProxyProperties;
import org.noear.solon.cloud.gateway.properties.TimeoutProperties;
import org.noear.solon.cloud.gateway.properties.XForwardedProperties;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.cloud.utils.CloudURI;
import org.noear.solon.rx.Completable;
import org.noear.solon.rx.CompletableEmitter;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.core.util.KeyValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Http 路由处理器
 *
 * @author noear
 * @since 2.9
 */
public class HttpRouteHandler implements RouteHandler {
    static final Logger log = LoggerFactory.getLogger(HttpRouteHandler.class);

    private final Vertx vertx;
    private final HttpClientProperties httpClientProps;
    private final XForwardedProperties xForwardedProps;
    private final Pattern nonProxyPattern; //proxy.nonProxyHostsPattern 预编译（非法正则/超长启动期 fail-fast）

    /**
     * 按上游 host 拆分的连接池（防单上游故障拖垮全局）
     *
     * <p>lb 场景下目标为具体实例地址，扩缩容/滚动发布/容器 IP 漂移会不断产生新 key，
     * 故以 pool.maxPools 为上限做 LRU 淘汰并关闭被淘汰客户端，防连接与本地内存累积泄漏。</p>
     */
    private final ConcurrentHashMap<String, PoolEntry> poolMap = new ConcurrentHashMap<>();
    private final int maxPools;

    public HttpRouteHandler(Vertx vertx) {
        this(vertx, new HttpClientProperties(), new XForwardedProperties());
    }

    public HttpRouteHandler(Vertx vertx, HttpClientProperties httpClientProps) {
        this(vertx, httpClientProps, new XForwardedProperties());
    }

    public HttpRouteHandler(Vertx vertx, HttpClientProperties httpClientProps, XForwardedProperties xForwardedProps) {
        this.vertx = vertx;
        this.httpClientProps = httpClientProps;
        this.xForwardedProps = xForwardedProps;
        this.nonProxyPattern = compileNonProxyPattern(httpClientProps);
        this.maxPools = Math.max(1, httpClientProps.getPool().getMaxPools());
    }

    /**
     * 连接池条目（带最近使用时间，用于 LRU 淘汰）
     */
    private static class PoolEntry {
        final HttpClient client;
        volatile long lastAccess;

        PoolEntry(HttpClient client) {
            this.client = client;
            this.lastAccess = System.currentTimeMillis();
        }
    }

    @Override
    public String[] schemas() {
        return new String[]{"http", "https"};
    }

    /**
     * 处理
     */
    @Override
    public Completable handle(ExContext ctx) {
        try {
            ctx.pause();

            //构建请求
            Future<HttpClientRequest> req1 = buildHttpRequest(ctx);

            return Completable.create(emitter -> {
                req1.onComplete(ar -> {
                    if (ar.succeeded()) {
                        handleDo(ctx, ar.result(), emitter);
                    } else {
                        emitter.onError(ar.cause());
                    }
                });
            });
        } catch (Throwable ex) {
            //如查出错，说明客户端发的数据有问题
            if (ex instanceof StatusException) {
                return Completable.error(ex);
            } else {
                return Completable.error(new StatusException(ex, 400));
            }
        }
    }

    public void handleDo(ExContext ctx, HttpClientRequest req1, CompletableEmitter emitter) {
        try {
            //逐跳头剥离集合（含 Connection 头显式声明的 token）
            Set<String> skipHeaders = ExHeaderUtils.buildSkipHeaders(ctx.rawHeader(ExConstants.Connection));

            //同步 header（剥离逐跳头；Host 不显式转发，由 Vert.x 按目标 URI 自动生成，原始值经 X-Forwarded-Host 传递）
            for (KeyValues<String> kv : ctx.newRequest().getHeaders()) {
                String key = kv.getKey();

                if (skipHeaders.contains(key.toLowerCase())) {
                    continue;
                }

                if (ExConstants.Host.equals(key)) {
                    continue;
                }

                req1.putHeader(key, kv.getValues());
            }

            //X-Forwarded-* 出站头统一生成（For/Host/Port/Proto）
            XForwardedHeaders.apply(xForwardedProps, ctx, req1.headers());

            //X-Real-IP 统一取 ctx.realIp()（其取值优先采信客户端 X-Real-IP / X-Forwarded-For，
            //故该值可被客户端伪造，仅作链路透传用，不可作为鉴权或审计依据；
            //需要不可伪造的对端地址时用 ctx.remoteAddress()）
            req1.putHeader(ExConstants.X_Real_IP, ctx.realIp());

            ExBody exBody = ctx.newRequest().getBody();

            //同步 body（流复制；未知 ExBody 实现按无体处理，防 ClassCastException 悬挂）
            if (exBody instanceof ExBodyOfBuffer) {
                Buffer buffer = ((ExBodyOfBuffer) exBody).getBuffer();

                //重置内容长度
                req1.putHeader(ExConstants.Content_Length, String.valueOf(buffer.length()));
                req1.send(buffer, ar1 -> {
                    callbackHandle(ctx, ar1, emitter);
                });
            } else if (exBody instanceof ExBodyOfStream) {
                //使用 chunked (也说明没有改过)
                req1.send(((ExBodyOfStream) exBody).getStream(), ar1 -> {
                    callbackHandle(ctx, ar1, emitter);
                });
            } else {
                req1.send(ar1 -> {
                    callbackHandle(ctx, ar1, emitter);
                });
            }

        } catch (Throwable ex) {
            //如查出错，说明客户端发的数据有问题
            if (ex instanceof StatusException) {
                emitter.onError(ex);
            } else {
                emitter.onError(new StatusException(ex, 400));
            }
        }
    }

    /**
     * 构建 http 请求对象
     */
    private Future<HttpClientRequest> buildHttpRequest(ExContext ctx) {
        RequestOptions requestOptions = new RequestOptions();

        //配置超时（无显式配置时使用全局默认；requestTimeout 为等待响应超时）
        TimeoutProperties timeout = ctx.timeout();
        if (timeout == null) {
            timeout = httpClientProps;
        }

        requestOptions.setConnectTimeout(timeout.getConnectTimeout() * 1000);
        requestOptions.setTimeout(timeout.getRequestTimeout() * 1000);

        //配置绝对地址
        requestOptions.setAbsoluteURI(ctx.targetNew() + ctx.newRequest().getPathAndQueryString());
        requestOptions.setMethod(HttpMethod.valueOf(ctx.newRequest().getMethod()));

        return getHttpClient(ctx).request(requestOptions);
    }

    /**
     * 按上游 host 获取独立连接池（走代理与直连的上游分池，防单上游故障拖垮全局）
     *
     * <p>池数量以 pool.maxPools 为上限，超限时淘汰最久未使用的池并关闭其客户端，
     * 防 lb 实例漂移导致 HttpClient 无界累积。</p>
     */
    private HttpClient getHttpClient(ExContext ctx) {
        CloudURI target = ctx.targetNew();
        String scheme = target.getRootScheme();
        String host = target.getHost();
        int port = target.getPort();
        if (port <= 0) {
            port = "https".equals(scheme) ? 443 : 80;
        }

        //代理/直连分池：命中 nonProxyHostsPattern 的上游 host 走直连（与走代理的 host 使用独立连接池）
        boolean useProxy = isProxyEnabledFor(host);
        String key = (useProxy ? "proxy|" : "direct|") + scheme + "://" + host + ":" + port;

        PoolEntry entry = poolMap.get(key);

        if (entry == null) {
            //仅创建路径加锁（读路径无锁）：保证淘汰与创建的原子性
            synchronized (poolMap) {
                entry = poolMap.get(key);

                if (entry == null) {
                    evictIfNeeded();
                    entry = new PoolEntry(vertx.createHttpClient(buildClientOptions(useProxy)));
                    poolMap.put(key, entry);
                }
            }
        }

        entry.lastAccess = System.currentTimeMillis();
        return entry.client;
    }

    /**
     * 池数量超限时淘汰最久未使用的池（调用方需持有 poolMap 锁）
     */
    private void evictIfNeeded() {
        while (poolMap.size() >= maxPools) {
            String oldestKey = null;
            long oldestAccess = Long.MAX_VALUE;

            for (Map.Entry<String, PoolEntry> kv : poolMap.entrySet()) {
                if (kv.getValue().lastAccess < oldestAccess) {
                    oldestAccess = kv.getValue().lastAccess;
                    oldestKey = kv.getKey();
                }
            }

            if (oldestKey == null) {
                return;
            }

            PoolEntry removed = poolMap.remove(oldestKey);
            if (removed != null) {
                //关闭为异步：进行中的请求由 Vert.x 自行结束
                try {
                    removed.client.close();
                } catch (Throwable ex) {
                    log.warn("Gateway http pool close failed: {}", oldestKey, ex);
                }
            }
        }
    }

    /**
     * 构建 http 客户端选项（连接池 + 压缩 + 代理 + SSL）
     */
    private HttpClientOptions buildClientOptions(boolean useProxy) {
        HttpClientOptions options = new HttpClientOptions()
                .setMaxPoolSize(httpClientProps.getPool().getMaxConnections())
                .setMaxWaitQueueSize(httpClientProps.getPool().getMaxWaitQueueSize()) //池等待队列上限，耗尽后快速失败（防无限排队悬挂）
                .setIdleTimeout(httpClientProps.getPool().getMaxIdleTime())
                .setKeepAlive(true)
                .setKeepAliveTimeout(httpClientProps.getPool().getKeepAliveTimeout())
                .setTryUseCompression(httpClientProps.isCompression()); //compression：出站 GZip

        //代理（nonProxyHostsPattern 命中的上游 host 走直连：useProxy=false 不装配）
        if (useProxy) {
            ClientOptionsUtil.applyProxy(httpClientProps, options);
        }

        //SSL（mTLS 客户端证书 / 自定义信任库）
        ClientOptionsUtil.applySsl(httpClientProps, options);

        return options;
    }

    /**
     * 预编译 nonProxyHostsPattern（非法正则/超长启动期 fail-fast，防运行时 ReDoS）
     */
    private static Pattern compileNonProxyPattern(HttpClientProperties props) {
        HttpProxyProperties proxy = props == null ? null : props.getProxy();
        if (proxy == null || Utils.isEmpty(proxy.getNonProxyHostsPattern())) {
            return null;
        }

        String pattern = proxy.getNonProxyHostsPattern();
        if (pattern.length() > 512) {
            throw new IllegalArgumentException("httpClient.proxy.nonProxyHostsPattern too long (max 512)");
        }

        return Pattern.compile(pattern);
    }

    /**
     * 目标 host 是否走代理（proxy.enabled + host 非空，且未命中 nonProxyHostsPattern）
     */
    private boolean isProxyEnabledFor(String host) {
        HttpProxyProperties proxy = httpClientProps.getProxy();
        if (proxy == null || !proxy.isEnabled() || Utils.isEmpty(proxy.getHost())) {
            return false;
        }

        if (nonProxyPattern == null) {
            return true;
        }

        //命中 nonProxyHostsPattern → 直连
        return !nonProxyPattern.matcher(host).matches();
    }

    /**
     * 请求回调处理
     */
    private void callbackHandle(ExContext ctx, AsyncResult<HttpClientResponse> ar, CompletableEmitter subscriber) {
        try {
            if (ar.succeeded()) {
                HttpClientResponse resp1 = ar.result();

                //code
                ctx.newResponse().status(resp1.statusCode());

                //header（与请求侧对称剥离逐跳头，含上游 Connection 声明的 token）：
                //防响应拆分/走私，并避免上游 Transfer-Encoding 与下游写出方式冲突
                Set<String> skipHeaders = ExHeaderUtils.buildSkipHeaders(resp1.getHeader(ExConstants.Connection));

                for (Map.Entry<String, String> kv : resp1.headers()) {
                    if (skipHeaders.contains(kv.getKey().toLowerCase())) {
                        continue;
                    }

                    ctx.newResponse().headerAdd(kv.getKey(), kv.getValue());
                }

                ctx.newResponse().body(resp1);

                subscriber.onComplete();
            } else {
                subscriber.onError(ar.cause());
            }
        } catch (Throwable ex) {
            subscriber.onError(ex);
        }
    }
}