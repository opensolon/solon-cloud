/*
 * Copyright 2017-2025 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.cloud.extend.opentelemetry.impl;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import org.noear.solon.Solon;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Condition;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;
import org.noear.solon.cloud.extend.opentelemetry.OpenTelemetryProps;
import org.noear.solon.core.AppContext;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


/**
 *
 * @author noear 2025/12/4 created
 * @since 3.7
 */
@Configuration
public class OpenTelemetryConfig {
    /// ////////////////////////
    /// trace
    ///

    @Condition(onMissingBean = SpanExporter.class)
    @Bean
    public SpanExporter spanExporter(OpenTelemetryProps cloudProps) {
        OtlpGrpcSpanExporter otlpExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(cloudProps.getServer()) //// 默认连接到 http://localhost:4317
                .setTimeout(30, TimeUnit.SECONDS)
                .build();

        return otlpExporter;
    }

    @Condition(onMissingBean = SdkTracerProvider.class)
    @Bean
    public SdkTracerProvider tracerProvider(Resource serviceResource,
                                            SpanExporter spanExporter) {
        BatchSpanProcessor spanProcessor = BatchSpanProcessor.builder(spanExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .build();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(serviceResource)
                .addSpanProcessor(spanProcessor)
                .setSampler(Sampler.alwaysOn()) // 设置采样器 (Sampler.alwaysOn() 表示 100% 采样)
                .build();

        return tracerProvider;
    }

    /// ////////////////////////
    /// meter
    ///
    @Condition(onMissingBean = MetricExporter.class)
    @Bean
    public MetricExporter metricExporter(OpenTelemetryProps cloudProps){
        OtlpGrpcMetricExporter otlpExporter = OtlpGrpcMetricExporter.builder()
                .setEndpoint(cloudProps.getServer())
                .setTimeout(Duration.ofSeconds(30))
                .build();

        return otlpExporter;
    }

    @Condition(onMissingBean = SdkMeterProvider.class)
    @Bean
    public SdkMeterProvider meterProvider(Resource serviceResource,
                                          MetricExporter metricExporter) {
        MetricReader metricReader = PeriodicMetricReader.builder(metricExporter)
                .setInterval(Duration.ofSeconds(10))
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .setResource(serviceResource)
                .registerMetricReader(metricReader)
                .build();

        return meterProvider;
    }

    /// ////////////////////////
    /// logger
    ///
    @Condition(onMissingBean = LogRecordExporter.class)
    @Bean
    public LogRecordExporter logExporter(OpenTelemetryProps cloudProps) {
        OtlpGrpcLogRecordExporter logRecordExporter = OtlpGrpcLogRecordExporter.builder()
                .setEndpoint(cloudProps.getServer())
                .setTimeout(30, TimeUnit.SECONDS)
                .build();
        return logRecordExporter;
    }

    @Condition(onMissingBean = SdkLoggerProvider.class)
    @Bean
    public SdkLoggerProvider loggerProvider(Resource serviceResource,
                                            LogRecordExporter recordExporter) {
        BatchLogRecordProcessor logRecordProcessor = BatchLogRecordProcessor.builder(recordExporter)
                .setScheduleDelay(100, TimeUnit.MILLISECONDS)
                .build();

        SdkLoggerProvider meterProvider = SdkLoggerProvider.builder()
                .setResource(serviceResource)
                .addLogRecordProcessor(logRecordProcessor)
                .build();

        return meterProvider;
    }

    /// ////////////////////////


    @Condition(onMissingBean = Resource.class)
    @Bean
    public Resource serviceResource() {
        Resource serviceResource = Resource.getDefault()
                .toBuilder()
                .put(ServiceAttributes.SERVICE_NAME, Solon.cfg().appName())
                .put(ServiceAttributes.SERVICE_VERSION, Solon.cfg().appVersion())
                .build();
        return serviceResource;
    }

    @Condition(onMissingBean = OpenTelemetry.class)
    @Bean
    public OpenTelemetry openTelemetry(AppContext context,
                                       @Inject(required = false) SdkTracerProvider tracerProvider,
                                       @Inject(required = false) SdkMeterProvider meterProvider,
                                       @Inject(required = false) SdkLoggerProvider loggerProvider,
                                       OpenTelemetryProps cloudProps) {

        if (cloudProps.getTraceEnable() == false) {
            tracerProvider = null;
        }

        if (cloudProps.getLogEnable() == false) {
            loggerProvider = null;
        }

        if (cloudProps.getMetricEnable() == false) {
            meterProvider = null;
        }


        OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .buildAndRegisterGlobal();

        return openTelemetrySdk;
    }
}