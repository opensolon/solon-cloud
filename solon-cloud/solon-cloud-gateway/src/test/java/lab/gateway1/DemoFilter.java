package lab.gateway1;

import io.vertx.core.buffer.Buffer;
import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.rx.Completable;

@Component
public class DemoFilter implements CloudGatewayFilter {
    @Override
    public Completable doFilter(ExContext ctx, ExFilterChain chain) {
        if (ctx.rawPath().equals("/demo/error")) {
            //模拟异常并转换（直接返回）
            return Completable.error(new RuntimeException("xxx"))
                    .doOnErrorResume(err -> {
                        ctx.newResponse().status(413);
                        ctx.newResponse().body(Buffer.buffer("hello"));
                        return Completable.complete();
                    });
        }

        if (ctx.rawPath().equals("/demo/body") && "1".equals(ctx.rawQueryParam("r"))) {
            //模拟 body 修改（一定要去掉 "Content-Length"）
            ctx.newRequest().headerRemove("Content-Length");
            ctx.newRequest().body(Buffer.buffer("hello-测试"));
        }

        return chain.doFilter(ctx);
    }
}
