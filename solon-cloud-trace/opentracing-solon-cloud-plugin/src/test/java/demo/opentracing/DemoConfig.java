package demo.opentracing;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.metrics.Metrics;
import io.jaegertracing.internal.metrics.NoopMetricsFactory;
import io.jaegertracing.internal.reporters.CompositeReporter;
import io.jaegertracing.internal.reporters.LoggingReporter;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.UdpSender;
import io.opentracing.Tracer;
import org.apache.thrift.transport.TTransportException;
import org.noear.solon.Solon;
import org.noear.solon.Utils;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.core.AppContext;

import java.net.URI;

@Configuration
public class DemoConfig {
    //构建跟踪服务。添加依赖：io.jaegertracing:jaeger-client:1.7.0
    @Bean
    public Tracer tracer(AppContext context) throws TTransportException {
        CloudProps cloudProps = new CloudProps(context,"opentracing");

        //为了可自由配置，这行代码重要
        if(cloudProps.getTraceEnable() == false
                || Utils.isEmpty(cloudProps.getServer())){
            return null;
        }

        URI serverUri = URI.create(cloudProps.getServer());
        Sender sender = new UdpSender(serverUri.getHost(), serverUri.getPort(), 0);

        final CompositeReporter compositeReporter = new CompositeReporter(
                new RemoteReporter.Builder().withSender(sender).build(),
                new LoggingReporter()
        );

        final Metrics metrics = new Metrics(new NoopMetricsFactory());

        return new JaegerTracer.Builder(Solon.cfg().appName())
                .withReporter(compositeReporter)
                .withMetrics(metrics)
                .withExpandExceptionLogs()
                .withSampler(new ConstSampler(true)).build();
    }
}
