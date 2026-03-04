package com.guicedee.telemetry.implementations;

import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import com.guicedee.client.utils.LogUtils;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
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

/**
 * Configures and initializes the OpenTelemetry SDK based on {@link TelemetryOptions}.
 *
 * <p>Supports OTLP HTTP exporters for production and in-memory exporters for testing.
 * The SDK is initialized once and made available via {@link #getOpenTelemetry()}.</p>
 */
@Log4j2
public class OpenTelemetrySDKConfigurator {

    private static OpenTelemetry openTelemetry;

    @Getter
    private static InMemorySpanExporter inMemorySpanExporter;
    @Getter
    private static InMemoryLogRecordExporter inMemoryLogRecordExporter;

    /**
     * Initializes the OpenTelemetry SDK if not already initialized.
     *
     * <p>Reads configuration from {@link TelemetryPreStartup#getOptions()},
     * creates exporters, tracer and logger providers, and registers SPI hooks.</p>
     */
    public static synchronized void initialize() {
        if (openTelemetry != null) {
            return;
        }

        TelemetryOptions options = TelemetryPreStartup.getOptions();
        String serviceName = "GuicedEE-Service";
        String serviceVersion = "1.0.0";
        String deploymentEnvironment = "production";
        String endpoint = "http://localhost:4318";
        boolean enabled = true;
        boolean useInMemory = false;
        boolean configureLogs = true;
        boolean logSignals = false;
        int maxBatchSize = 512;
        int maxLogBatchSize = 512;

        if (options != null) {
            enabled = options.enabled();
            useInMemory = options.useInMemoryExporters();
            configureLogs = options.configureLogs();
            logSignals = options.logSignals();
            serviceVersion = options.serviceVersion();
            deploymentEnvironment = options.deploymentEnvironment();
            maxBatchSize = options.maxBatchSize();
            maxLogBatchSize = options.maxLogBatchSize();
            if (options.serviceName() != null && !options.serviceName().isBlank()) {
                serviceName = options.serviceName();
            }
            if (options.otlpEndpoint() != null && !options.otlpEndpoint().isBlank()) {
                endpoint = options.otlpEndpoint();
            }
            log.info("Options found: enabled={}, useInMemory={}, serviceName={}, endpoint={}", enabled, useInMemory, serviceName, endpoint);
        } else {
            log.info("No options found, using defaults");
        }

        // System property wins for endpoint IF NOT in memory mode
        if (!useInMemory) {
            endpoint = com.guicedee.client.Environment.getSystemPropertyOrEnvironment("OTEL_EXPORTER_OTLP_ENDPOINT", endpoint);
        }

        log.info("Final telemetry endpoint: {}, Use InMemory: {}", endpoint, useInMemory);

        if (!enabled) {
            log.info("Telemetry is disabled via options.");
            openTelemetry = OpenTelemetry.noop();
            return;
        }

        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        AttributeKey.stringKey("service.name"), serviceName,
                        AttributeKey.stringKey("service.version"), serviceVersion,
                        AttributeKey.stringKey("deployment.environment"), deploymentEnvironment,
                        AttributeKey.stringKey("host.name"), com.guicedee.client.Environment.getSystemPropertyOrEnvironment("HOSTNAME", "localhost")
                )));

        SpanExporter spanExporter;
        LogRecordExporter logRecordExporter;

        if (useInMemory) {
            inMemorySpanExporter = InMemorySpanExporter.create();
            inMemoryLogRecordExporter = InMemoryLogRecordExporter.create();
            spanExporter = inMemorySpanExporter;
            logRecordExporter = inMemoryLogRecordExporter;
        } else {
            System.setProperty("io.opentelemetry.exporter.internal.http.HttpSenderProvider", "io.opentelemetry.exporter.sender.jdk.internal.JdkHttpSenderProvider");
            spanExporter = OtlpHttpSpanExporter.builder().setEndpoint(endpoint).build();
            logRecordExporter = OtlpHttpLogRecordExporter.builder().setEndpoint(endpoint).build();
        }

        log.info("Using {} span exporter", useInMemory ? "InMemory" : "OTLP HTTP to " + endpoint);

        if (logSignals) {
            log.info("Enabling signal logging (OTel internal logging)");
            System.setProperty("otel.java.logging.exporter.enabled", "true");
            // Also set the level for the OTLP exporter loggers if possible
            java.util.logging.Logger.getLogger("io.opentelemetry.exporter.otlp").setLevel(java.util.logging.Level.FINEST);
            java.util.logging.Logger.getLogger("io.opentelemetry.sdk.trace.export").setLevel(java.util.logging.Level.FINEST);
            java.util.logging.Logger.getLogger("io.opentelemetry.sdk.logs.export").setLevel(java.util.logging.Level.FINEST);
        }

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(useInMemory ? SimpleSpanProcessor.create(spanExporter) : BatchSpanProcessor.builder(spanExporter)
                        .setMaxExportBatchSize(maxBatchSize)
                        .build())
                .setResource(resource)
                .build();

        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(useInMemory ? SimpleLogRecordProcessor.create(logRecordExporter) : BatchLogRecordProcessor.builder(logRecordExporter)
                        .setMaxExportBatchSize(maxLogBatchSize)
                        .build())
                .setResource(resource)
                .build();

        if (!useInMemory) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("Shutting down OpenTelemetry SDK...");
                if (openTelemetry instanceof OpenTelemetrySdk) {
                    ((OpenTelemetrySdk) openTelemetry).close();
                }
            }));
        }

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
        // System.out.println("[DEBUG_LOG] OpenTelemetrySDKConfigurator INITIALIZED, SDK: " + sdk + ", openTelemetry: " + openTelemetry);

        if (configureLogs) {
            LogUtils.addHighlightedConsoleLogger(org.apache.logging.log4j.Level.INFO);
            try {
                org.apache.logging.log4j.core.LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
                if (context.getConfiguration().getAppender("OpenTelemetryAppender") != null) {
                    context.getConfiguration().getAppenders().remove("OpenTelemetryAppender");
                }

                Class<?> appenderClass = Class.forName("io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender");
                var builderMethod = appenderClass.getMethod("builder");
                var builder = builderMethod.invoke(null);
                builder.getClass().getMethod("setName", String.class).invoke(builder, "OpenTelemetryAppender");
                // Set the OpenTelemetry instance directly if possible
                try {
                     builder.getClass().getMethod("setOpenTelemetry", OpenTelemetry.class).invoke(builder, openTelemetry);
                } catch (Exception ignore) {}

                org.apache.logging.log4j.core.Appender appender = (org.apache.logging.log4j.core.Appender) builder.getClass().getMethod("build").invoke(builder);
                LogUtils.addAppender(appender, org.apache.logging.log4j.Level.ALL);
                log.info("OpenTelemetry Log4j2 Appender registered.");
            } catch (Exception e) {
                log.warn("Could not register OpenTelemetry Log4j2 Appender. Ensure opentelemetry-log4j-appender is on classpath.", e);
            }
        }
        ServiceLoader<GuiceTelemetryRegistration> registrations = ServiceLoader.load(GuiceTelemetryRegistration.class);
        for (GuiceTelemetryRegistration registration : registrations) {
             openTelemetry = registration.configure(openTelemetry);
        }

        log.info("OpenTelemetry SDK initialized for service: {}", serviceName);
    }

    /**
     * Returns the configured OpenTelemetry instance, initializing it if necessary.
     *
     * @return the OpenTelemetry instance
     */
    public static OpenTelemetry getOpenTelemetry() {
        if (openTelemetry == null) {
            initialize();
        }
        return openTelemetry;
    }

    /**
     * Shuts down and resets the OpenTelemetry SDK, clearing all state.
     */
    public static synchronized void reset() {
        // log.debug("OpenTelemetrySDKConfigurator RESET", new RuntimeException("Reset Stack Trace"));
        if (openTelemetry instanceof OpenTelemetrySdk) {
            ((OpenTelemetrySdk) openTelemetry).close();
        }
        openTelemetry = null;
        inMemorySpanExporter = null;
        inMemoryLogRecordExporter = null;
        System.clearProperty("OTEL_EXPORTER_OTLP_ENDPOINT");
        System.clearProperty("io.opentelemetry.exporter.internal.http.HttpSenderProvider");
        TelemetryPreStartup.reset();
        try {
            GlobalOpenTelemetry.resetForTest();
        } catch (Exception e) {
            // log.debug("Error resetting GlobalOpenTelemetry", e);
        }
        try {
            org.apache.logging.log4j.core.LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
            context.getConfiguration().getAppenders().remove("OpenTelemetryAppender");
            context.updateLoggers();
        } catch (Exception e) {
             // ignore
        }
        // System.out.println("[DEBUG_LOG] OpenTelemetrySDKConfigurator RESET");
    }

    /**
     * Returns whether the SDK has been reset (i.e. not currently initialized).
     *
     * @return {@code true} if the SDK is not initialized
     */
    public static boolean isReset() {
        return openTelemetry == null;
    }
}
