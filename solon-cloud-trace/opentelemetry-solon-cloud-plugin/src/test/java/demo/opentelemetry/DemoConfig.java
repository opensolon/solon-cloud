package demo.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.cloud.CloudProps;
import org.noear.solon.core.AppContext;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author noear 2025/11/30 created
 *
 */
@Configuration
public class DemoConfig {
    @Bean
    public OpenTelemetry openTelemetry(AppContext context) {
        CloudProps cloudProps = new CloudProps(context, "opentelemetry");

        // 1. 定义资源 (Resource)，包含服务名称
        Resource serviceResource = Resource.getDefault()
                .toBuilder()
                .put(ServiceAttributes.SERVICE_NAME, Solon.cfg().appName())
                .put(ServiceAttributes.SERVICE_VERSION,"1.0.0")
                .build();

        // 2. 配置 Span Exporter (例如 OTLP/gRPC)
        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
                // 默认连接到 http://localhost:4317 (OTLP Collector)
                .setEndpoint(cloudProps.getServer())
                .setTimeout(30, TimeUnit.SECONDS)
                .build();

        // 3. 配置 Span Processor (例如 BatchSpanProcessor)
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(otlpExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .build();

        // 4. 配置 Tracer Provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(serviceResource)
                .addSpanProcessor(spanProcessor)
                .setSampler(Sampler.alwaysOn()) // 设置采样器 (Sampler.alwaysOn() 表示 100% 采样)
                .build();

        // 5. 构建 OpenTelemetry SDK 并注册为全局单例
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        System.out.println("OpenTelemetry SDK initialized and set as global.");
        return openTelemetrySdk;
    }
}
