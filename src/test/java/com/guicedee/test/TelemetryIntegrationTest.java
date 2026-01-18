package com.guicedee.test;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.guicedee.client.IGuiceContext;
import com.guicedee.telemetry.annotations.Telemetry;
import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import com.guicedee.telemetry.implementations.TelemetryPreStartup;
import com.guicedee.telemetry.implementations.TraceModule;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.data.SpanData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Telemetry(serviceName = "IntegrationTest", useInMemoryExporters = true)
public class TelemetryIntegrationTest {

    private static final Logger logger = LogManager.getLogger(TelemetryIntegrationTest.class);

    private InMemorySpanExporter spanExporter;
    private InMemoryLogRecordExporter logExporter;

    @BeforeEach
    public void setup() {
        OpenTelemetrySDKConfigurator.reset();
        IGuiceContext.instance().getConfig().setClasspathScanning(true)
            .setAnnotationScanning(true).setFieldScanning(true).setIgnoreFieldVisibility(true).setIgnoreMethodVisibility(true).setIgnoreClassVisibility(true);

        IGuiceContext.instance().getScanResult(); // Ensure context is initialized
        new TelemetryPreStartup().onStartup(); // Manually trigger pre-startup to pick up @Telemetry
        spanExporter = OpenTelemetrySDKConfigurator.getInMemorySpanExporter();
        logExporter = OpenTelemetrySDKConfigurator.getInMemoryLogRecordExporter();
    }

    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testFullTracingAndLogging() {
        TracedService service = IGuiceContext.get(TracedService.class);

        service.doWork();

        // Verify Span
        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size(), "Should have captured one span");
        SpanData span = spans.get(0);
        assertEquals("TestSpan", span.getName());

        // Verify Log
        List<LogRecordData> logs = logExporter.getFinishedLogRecordItems();
        assertFalse(logs.isEmpty(), "Should have captured at least one log");

        LogRecordData logRecord = logs.stream()
            .filter(l -> l.getBodyValue() != null && l.getBodyValue().asString().contains("Doing some work"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected log message not found. Captured logs: " + logs));

        // Verify association
        assertEquals(span.getSpanContext().getTraceId(), logRecord.getSpanContext().getTraceId(), "Trace ID should match");
        assertEquals(span.getSpanContext().getSpanId(), logRecord.getSpanContext().getSpanId(), "Span ID should match");

        System.out.println("Verified Span: " + span.getName() + " [" + span.getSpanContext().getTraceId() + "]");
        System.out.println("Verified Log: " + logRecord.getBodyValue().asString() + " [" + logRecord.getSpanContext().getTraceId() + "]");
    }

    @Test
    public void testSpanAttributes() {
        TracedService service = IGuiceContext.get(TracedService.class);

        service.methodWithAttributes("world", 42, new java.util.HashMap<>(java.util.Map.of("key", "value")));

        List<SpanData> spans = spanExporter.getFinishedSpanItems();
        SpanData span = spans.stream().filter(s -> s.getName().equals("AttributeSpan")).findFirst().orElseThrow();

        assertEquals("world", span.getAttributes().get(AttributeKey.stringKey("param1")));
        assertEquals(42L, span.getAttributes().get(AttributeKey.longKey("p2")));
        assertEquals("Hello world 42", span.getAttributes().get(AttributeKey.stringKey("my_result")));
    }

    @Test
    public void testTempoIntegration() {
        // This test attempts to send a real OTLP signal to localhost:4317
        // It's mostly to verify that the SDK can be initialized with OTLP and doesn't crash
        // If Tempo is running in docker-compose, this will actually send data there.

        System.setProperty("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");

        // Use the real configurator but with a custom service name
        // We need to reset the openTelemetry field first if it was already initialized
        OpenTelemetrySDKConfigurator.reset();

        OpenTelemetrySDKConfigurator.initialize();
        OpenTelemetry otel = OpenTelemetrySDKConfigurator.getOpenTelemetry();

        io.opentelemetry.api.trace.Tracer tracer = otel.getTracer("TempoTest");
        Span span = tracer.spanBuilder("TempoIntegrationSpan").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("test.type", "tempo-integration");
            logger.info("Sending trace to Tempo at http://localhost:4317");
        } finally {
            span.end();
        }

        // Wait a bit for batch processor to export
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertNotNull(otel);
    }
}
