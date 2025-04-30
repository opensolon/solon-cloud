package features.gateway.sys;

import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.core.handle.Context;
import org.noear.solon.rx.Completable;
import org.noear.solon.web.cors.CrossHandler;

/**
 * @author noear 2025/4/30 created
 */
@Component(index = -99)
public class CloudGatewayCorsFilter implements CloudGatewayFilter {
    private CrossHandler crossHandler = new CrossHandler();

    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        Context ctx2 = ctx.toContext();

        try {
            crossHandler.handle(ctx2);

            if (ctx2.getHandled()) {
                return Completable.complete();
            } else {
                return chain.doFilter(ctx);
            }
        } catch (Throwable ex) {
            return Completable.error(ex);
        }
    }
}
