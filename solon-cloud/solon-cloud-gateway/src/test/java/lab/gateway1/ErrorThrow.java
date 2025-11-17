package lab.gateway1;

import io.vertx.core.buffer.Buffer;
import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.rx.Completable;

@Component
public class ErrorThrow implements CloudGatewayFilter {
    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        if (ctx.newRequest().getPath().equals("/demo/error")) {
            //模异常
            return Completable.error(new RuntimeException("xxx"))
                    .doOnErrorResume(err -> {
                        ctx.newResponse().status(413);
                        ctx.newResponse().body(Buffer.buffer("hello"));
                        return Completable.complete();
                    });
        }

        return chain.doFilter(ctx);
    }
}
