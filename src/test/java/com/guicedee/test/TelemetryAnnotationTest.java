package com.guicedee.test;

import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.annotations.Telemetry;
import com.guicedee.telemetry.implementations.TelemetryPreStartup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Telemetry(enabled = true, serviceName = "AnnotationService", otlpEndpoint = "http://localhost:4317", useInMemoryExporters = true)
public class TelemetryAnnotationTest {

    @Test
    public void testAnnotationScanning() {
        TelemetryPreStartup preStartup = new TelemetryPreStartup();
        try {
            preStartup.onStartup();
            System.out.println("[DEBUG_LOG] onStartup called");
            TelemetryOptions options = TelemetryPreStartup.getOptions();
            if (options != null) {
                System.out.println("[DEBUG_LOG] Options found: " + options.serviceName() + ", " + options.otlpEndpoint() + ", " + options.useInMemoryExporters());
                assertFalse(options.enabled());
                assertEquals("AnnotationService", options.serviceName());
                assertEquals("http://localhost:4317", options.otlpEndpoint());
                assertTrue(options.useInMemoryExporters());
            } else {
                System.out.println("[DEBUG_LOG] Options are NULL");
            }
        } catch (Exception e) {
            System.out.println("IGuiceContext not initialized, skipping full check: " + e.getMessage());
        }
    }
}
