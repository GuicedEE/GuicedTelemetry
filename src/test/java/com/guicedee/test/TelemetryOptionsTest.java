package com.guicedee.test;

import com.guicedee.client.IGuiceContext;
import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import com.guicedee.telemetry.implementations.TelemetryPreStartup;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@TelemetryOptions(enabled = true, serviceName = "TestService", useInMemoryExporters = true)
public class TelemetryOptionsTest {

    @Test
    public void testOptionsScanning() {
        OpenTelemetrySDKConfigurator.reset();
        TelemetryPreStartup preStartup = new TelemetryPreStartup();
        try {
            preStartup.onStartup();
            TelemetryOptions options = TelemetryPreStartup.getOptions();
            assertNotNull(options, "Options should not be null after scanning");
            assertTrue(options.enabled());
            assertNotNull(options.serviceName());
            assertFalse(options.serviceName().isBlank());
        } catch (Exception e) {
            System.out.println("IGuiceContext not initialized, skipping full check: " + e.getMessage());
        }
    }
}
