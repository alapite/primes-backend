package ereh.won.otbackend;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OpenTelemetryConfig {
    @Value("${otel.service.name}")
    private String applicationName;
    @Value("${otel.service.version}")
    private String applicationVersion;
    @Value("${otel.exporter.otlp.metrics.endpoint}")
    private String metricsOtlpEndpoint;
    @Value("${otel.exporter.otlp.traces.endpoint}")
    private String tracesOtlpEndpoint;
    @Value("${otel.exporter.otlp.logs.endpoint}")
    private String logsOtlpEndpoint;

    @Bean
    public Resource otelResource() {
        return Resource.getDefault()
                .merge(
                        Resource.create(
                                Attributes.of(
                                        ServiceAttributes.SERVICE_NAME, applicationName,
                                        ServiceAttributes.SERVICE_VERSION, applicationVersion
                                )
                        )
                );
    }

    @Bean(name = "backendOpenTelemetry")
    public OpenTelemetry openTelemetry(Resource resource) {
        // OtlpHttpSpanExporter is used to export traces to an OpenTelemetry collector
        log.info("Traces Endpoint = {}", tracesOtlpEndpoint);
        OtlpHttpSpanExporter traceHttpSpanExporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(tracesOtlpEndpoint)
                .build();

        // OtlpHttpMetricExporter is used to export metrics to an OpenTelemetry collector
        log.info("Metrics Endpoint = {}", metricsOtlpEndpoint);
        OtlpHttpMetricExporter metricExporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(metricsOtlpEndpoint)
                .build();

        // SdkTracerProvider is used to create and configure the tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(traceHttpSpanExporter))
                .setResource(resource)
                .build();

        // SdkMeterProvider is used to create and configure the meter provider
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(metricExporter).build())
                .setResource(resource)
                .build();

        log.info("Logs Endpoint = {}", logsOtlpEndpoint);
        LogRecordExporter logRecordExporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(logsOtlpEndpoint)
                .build();
        LogRecordProcessor logRecordProcessor = BatchLogRecordProcessor
                .builder(logRecordExporter).build();
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(logRecordProcessor)
                .build();

        return OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .setPropagators(
                        ContextPropagators.create(
                                TextMapPropagator.composite(
                                        W3CTraceContextPropagator.getInstance(),
                                        W3CBaggagePropagator.getInstance()
                                )
                        )
                )
                .build();

    }
}
