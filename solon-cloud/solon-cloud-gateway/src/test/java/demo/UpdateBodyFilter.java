package demo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientResponse;
import org.noear.solon.Utils;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExBody;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfStream;
import org.noear.solon.rx.Completable;

/**
 *
 * @author noear 2025/11/17 created
 *
 */
public class UpdateBodyFilter implements CloudGatewayFilter {
    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        return chain.doFilter(ctx).then(Completable.create(emitter -> {
            ExBody exBody = ctx.newResponse().getBody();
            if (exBody instanceof ExBodyOfStream) {
                ExBodyOfStream streamBody = ((ExBodyOfStream) exBody);
                ((HttpClientResponse) streamBody.getStream()).body().andThen(bodyAr -> {
                    if (bodyAr.succeeded()) {
                        // 获取响应体内容
                        String content = bodyAr.result().toString();
                        ctx.newResponse().header("MD5", Utils.md5(content));
                        ctx.newResponse().body(Buffer.buffer(content + "#demo"));
                        emitter.onComplete();
                    } else {
                        emitter.onError(bodyAr.cause());
                    }
                });
            }
        }));
    }
}
