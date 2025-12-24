package features.gateway.ctx;

import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Result;
import org.noear.solon.rx.Completable;

/**
 * @author noear 2024/12/18 created
 */
@Component
public class AppFilter implements CloudGatewayFilter {
    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        return Context.currentWith(ctx.toContext(), ctx2 -> {
            try {
                //经典处理
                classicalHandle(ctx2);

                if (ctx2.isHeadersSent()) {
                    ctx2.flush();
                    return Completable.complete();
                }

                return chain.doFilter(ctx);
            } catch (Throwable ex) {
                return Completable.error(ex);
            }
        });
    }

    private void classicalHandle(Context ctx) throws Throwable {
        int s1 = ctx.paramAsInt("s", 0);

        if (s1 == 1) {
            throw new RuntimeException("s1");
        } else if (s1 == 2) {
            ctx.render(Result.failure());
        } else if (s1 == 3) {
            ctx.output("err");
        }
    }
}
