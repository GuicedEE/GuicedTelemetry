package com.guicedee.test;

import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import com.guicedee.telemetry.implementations.TelemetryPreStartup;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TelemetryAppenderTest {
    private LoggerContext context;

    @BeforeEach
    void initialize() {
        OpenTelemetrySDKConfigurator.reset();
        TelemetryPreStartup.setOptions((TelemetryOptions) java.lang.reflect.Proxy.newProxyInstance(
                TelemetryOptions.class.getClassLoader(), new Class<?>[]{TelemetryOptions.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "annotationType" -> TelemetryOptions.class;
                    case "serviceName" -> "appender-test";
                    case "useInMemoryExporters" -> true;
                    default -> method.getDefaultValue();
                }));
        OpenTelemetrySDKConfigurator.initialize();
        context = (LoggerContext) LogManager.getContext(false);
    }

    @AfterEach
    void reset() {
        OpenTelemetrySDKConfigurator.reset();
    }

    @Test
    void sdkDiagnosticsRemainLocalAndCannotReenterTelemetryAppender() {
        var appender = (OpenTelemetryAppender) context.getConfiguration().getAppender("OpenTelemetryAppender");
        List<String> errors = new ArrayList<>();
        appender.setHandler(new ErrorHandler() {
            @Override public void error(String message) { errors.add(message); }
            @Override public void error(String message, Throwable error) { errors.add(message); }
            @Override public void error(String message, LogEvent event, Throwable error) { errors.add(message); }
        });
        List<String> localMessages = new ArrayList<>();
        var local = new AbstractAppender("telemetry-test-local", null, null, true, Property.EMPTY_ARRAY) {
            @Override public void append(LogEvent event) { localMessages.add(event.getMessage().getFormattedMessage()); }
        };
        local.start();
        context.getConfiguration().getRootLogger().addAppender(local, Level.ALL, null);
        var captured = InMemoryLogRecordExporter.create();
        var exporter = new LogRecordExporter() {
            @Override public CompletableResultCode export(Collection<LogRecordData> records) {
                // A diagnostic emitted synchronously from within the appender/SDK call.
                LogManager.getLogger("io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor")
                        .warn("nested SDK diagnostic");
                return captured.export(records);
            }
            @Override public CompletableResultCode flush() { return captured.flush(); }
            @Override public CompletableResultCode shutdown() { return captured.shutdown(); }
        };
        var sdk = OpenTelemetrySdk.builder().setLoggerProvider(SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(exporter)).build()).build();
        appender.setOpenTelemetry(sdk);
        try {
            LogManager.getLogger("application.telemetry.test").warn("application record");
            // A diagnostic emitted outside the appender call must not start another export either.
            LogManager.getLogger("io.opentelemetry.exporter.internal.http.HttpExporter")
                    .warn("export failure diagnostic");
            assertTrue(errors.isEmpty(), "Appender errors: " + errors);
            assertEquals(List.of("application record"), captured.getFinishedLogRecordItems().stream()
                    .map(record -> record.getBodyValue().asString()).toList());
            assertTrue(localMessages.containsAll(List.of("application record", "nested SDK diagnostic", "export failure diagnostic")));
        } finally {
            context.getConfiguration().getRootLogger().removeAppender(local.getName());
            local.stop();
            OpenTelemetrySDKConfigurator.reset();
            sdk.close();
        }
    }

    @Test
    void resetDetachesAndStopsAppenderFromRootAndNamedLoggers() {
        var configuration = context.getConfiguration();
        var appender = configuration.getAppender("OpenTelemetryAppender");
        var named = new LoggerConfig("telemetry.lifecycle.test", Level.INFO, false);
        named.addAppender(appender, Level.ALL, null);
        configuration.addLogger(named.getName(), named);
        context.updateLoggers();
        try {
            OpenTelemetrySDKConfigurator.reset();
            assertFalse(configuration.getRootLogger().getAppenders().containsKey(appender.getName()));
            assertFalse(named.getAppenders().containsKey(appender.getName()));
            assertNull(configuration.getAppender(appender.getName()));
            assertTrue(appender.isStopped());
        } finally {
            configuration.removeLogger(named.getName());
            context.updateLoggers();
        }
    }

    @Test
    void repeatedInitializationExportsEachApplicationLogOnce() {
        for (int cycle = 0; cycle < 3; cycle++) {
            if (cycle > 0) {
                initialize();
            }
            var exporter = OpenTelemetrySDKConfigurator.getInMemoryLogRecordExporter();
            exporter.reset();
            LogManager.getLogger("application.telemetry.test").warn("cycle " + cycle);
            assertEquals(List.of("cycle " + cycle), exporter.getFinishedLogRecordItems().stream()
                    .map(record -> record.getBodyValue().asString()).toList());
        }
    }
}
