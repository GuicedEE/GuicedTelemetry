package com.guicedee.telemetry.implementations;

import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import com.guicedee.client.utils.LogUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.ServiceLoader;

@Log4j2
public class OpenTelemetrySDKConfigurator {

    private static OpenTelemetry openTelemetry;

    @Getter
    private static InMemorySpanExporter inMemorySpanExporter;
    @Getter
    private static InMemoryLogRecordExporter inMemoryLogRecordExporter;

    public static synchronized void initialize() {
        if (openTelemetry != null) {
            return;
        }

        TelemetryOptions options = TelemetryPreStartup.getOptions();
        String serviceName = "GuicedEE-Service";
        String endpoint = "http://localhost:4317";
        boolean enabled = true;
        boolean useInMemory = false;
        boolean configureLogs = true;

        if (options != null) {
            enabled = options.enabled();
            useInMemory = options.useInMemoryExporters();
            configureLogs = options.configureLogs();
            if (!options.serviceName().isBlank()) {
                serviceName = options.serviceName();
            }
            if (!options.otlpEndpoint().isBlank()) {
                endpoint = options.otlpEndpoint();
            }
        }

        String envEndpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (envEndpoint != null && !envEndpoint.isBlank()) {
            endpoint = envEndpoint;
        }

        if (!enabled) {
            log.info("Telemetry is disabled via options.");
            openTelemetry = OpenTelemetry.noop();
            return;
        }

        if (configureLogs) {
            LogUtils.addHighlightedConsoleLogger(org.apache.logging.log4j.Level.INFO);
            try {
                Class<?> appenderClass = Class.forName("io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender");
                var builderMethod = appenderClass.getMethod("builder");
                var builder = builderMethod.invoke(null);
                builder.getClass().getMethod("setName", String.class).invoke(builder, "OpenTelemetryAppender");
                org.apache.logging.log4j.core.Appender appender = (org.apache.logging.log4j.core.Appender) builder.getClass().getMethod("build").invoke(builder);
                LogUtils.addAppender(appender, org.apache.logging.log4j.Level.ALL);
                log.info("OpenTelemetry Log4j2 Appender registered.");
            } catch (Exception e) {
                log.warn("Could not register OpenTelemetry Log4j2 Appender. Ensure opentelemetry-log4j-appender is on classpath.", e);
            }
        }

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)));

        SpanExporter spanExporter;
        LogRecordExporter logRecordExporter;

        if (useInMemory) {
            inMemorySpanExporter = InMemorySpanExporter.create();
            inMemoryLogRecordExporter = InMemoryLogRecordExporter.create();
            spanExporter = inMemorySpanExporter;
            logRecordExporter = inMemoryLogRecordExporter;
        } else {
            spanExporter = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
            logRecordExporter = OtlpGrpcLogRecordExporter.builder().setEndpoint(endpoint).build();
        }

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(useInMemory ? SimpleSpanProcessor.create(spanExporter) : BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(useInMemory ? SimpleLogRecordProcessor.create(logRecordExporter) : BatchLogRecordProcessor.builder(logRecordExporter).build())
                .setResource(resource)
                .build();

        // Allow SPI to further customize
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setLoggerProvider(loggerProvider)
                .build();
        try {
            GlobalOpenTelemetry.set(sdk);
        } catch (IllegalStateException e) {
            // Already set, nothing we can do but log it
            log.warn("GlobalOpenTelemetry already set, cannot register new SDK: {}", e.getMessage());
        }
        openTelemetry = sdk;
        ServiceLoader<GuiceTelemetryRegistration> registrations = ServiceLoader.load(GuiceTelemetryRegistration.class);
        for (GuiceTelemetryRegistration registration : registrations) {
             openTelemetry = registration.configure(openTelemetry);
        }

        log.info("OpenTelemetry SDK initialized for service: " + serviceName);
    }

    public static OpenTelemetry getOpenTelemetry() {
        if (openTelemetry == null) {
            initialize();
        }
        return openTelemetry;
    }

    public static synchronized void reset() {
        openTelemetry = null;
        inMemorySpanExporter = null;
        inMemoryLogRecordExporter = null;
        TelemetryPreStartup.reset();
        GlobalOpenTelemetry.resetForTest();
    }
}
