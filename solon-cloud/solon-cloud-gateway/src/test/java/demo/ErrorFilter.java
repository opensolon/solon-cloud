package demo;

import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.core.exception.StatusException;
import org.noear.solon.rx.Completable;

/**
 *
 * @author noear 2025/11/17 created
 *
 */
public class ErrorFilter implements CloudGatewayFilter {
    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        return chain.doFilter(ctx).doOnErrorResume(e -> {
            if (e instanceof StatusException) {
                StatusException se = (StatusException) e;

                ctx.newResponse().status(se.getCode());
            } else {
                ctx.newResponse().status(500);
            }

            return Completable.complete();
        });
    }
}
