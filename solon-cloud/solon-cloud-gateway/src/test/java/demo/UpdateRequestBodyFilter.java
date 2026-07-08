package demo;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.streams.ReadStream;
import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.gateway.CloudGatewayFilter;
import org.noear.solon.cloud.gateway.exchange.ExBody;
import org.noear.solon.cloud.gateway.exchange.ExContext;
import org.noear.solon.cloud.gateway.exchange.ExFilterChain;
import org.noear.solon.cloud.gateway.exchange.impl.ExBodyOfStream;
import org.noear.solon.rx.Completable;

import java.util.Base64;

@Component
public class UpdateRequestBodyFilter implements CloudGatewayFilter {
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

        if (ctx.rawPath().equals("/demo/body")) {
            //模拟 body 修改（一定要去掉 "Content-Length"）
            ctx.newRequest().headerRemove("Content-Length");

            ExBody exBody = ctx.newRequest().getBody();
            if (exBody instanceof ExBodyOfStream) {
                //读取 body，转码后再转发（内存占用会大大提交）
                ReadStream<Buffer> stream = ((ExBodyOfStream) exBody).getStream();

                return Completable.create(emitter -> {
                    ((HttpServerRequest) stream).body().andThen(bodyAr -> {
                        if (bodyAr.succeeded()) {
                            byte[] decoded = Base64.getDecoder().decode(bodyAr.result().getBytes());
                            ctx.newRequest().body(Buffer.buffer(decoded));
                            emitter.onComplete();
                        } else {
                            emitter.onError(bodyAr.cause());
                        }
                    });
                }).then(() -> chain.doFilter(ctx));
            } else {
                //直接修改 body
                ctx.newRequest().body(Buffer.buffer("hello-测试"));
            }
        }

        return chain.doFilter(ctx);
    }
}
