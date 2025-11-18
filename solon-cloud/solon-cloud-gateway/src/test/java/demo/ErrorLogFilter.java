package demo;

import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.rx.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author noear 2025/11/17 created
 */
public class ErrorLogFilter implements CloudGatewayFilter {
    static final Logger log = LoggerFactory.getLogger(ErrorLogFilter.class);

    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        return chain.doFilter(ctx).doOnError(e -> {
            log.error(e.getMessage());
        });
    }
}
