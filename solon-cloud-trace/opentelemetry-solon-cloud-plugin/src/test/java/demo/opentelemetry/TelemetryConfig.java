package demo.opentelemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author noear 2025/11/30 created
 *
 */
@Configuration
public class TelemetryConfig {
    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(Solon.cfg().appName(), "1.0.0");
    }

    @Bean
    public TextMapPropagator textMapPropagator(OpenTelemetry openTelemetry) {
        return openTelemetry.getPropagators().getTextMapPropagator();
    }

    @Bean
    public OpenTelemetry openTelemetry() {
        // 1. 定义资源 (Resource)，包含服务名称
        Resource serviceResource = Resource.getDefault()
                .toBuilder()
                .put("app.name", Solon.cfg().appName())
                .build();

        // 2. 配置 Span Exporter (例如 OTLP/gRPC)
        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
                // 默认连接到 http://localhost:4317 (OTLP Collector)
                // .setEndpoint("http://your-otel-collector:4317")
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
                // 设置采样器 (Sampler.alwaysOn() 表示 100% 采样)
                .setSampler(Sampler.alwaysOn())
                .build();

        // 5. 构建 OpenTelemetry SDK 并注册为全局单例
        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        System.out.println("OpenTelemetry SDK initialized and set as global.");
        return openTelemetrySdk;
    }
}
