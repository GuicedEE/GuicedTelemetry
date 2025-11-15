package com.guicedee.test;

import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the GuiceTelemetryRegistration SPI is discoverable via ServiceLoader
 * and that the default implementation returns the provided OpenTelemetry instance.
 */
public class TelemetrySpiTest {

    @Test
    public void testServiceLoaderFindsRegistration() {
        ServiceLoader<GuiceTelemetryRegistration> loader = ServiceLoader.load(GuiceTelemetryRegistration.class);
        GuiceTelemetryRegistration<?> impl = loader.findFirst().orElse(null);
        assertNotNull(impl, "Expected at least one GuiceTelemetryRegistration implementation via ServiceLoader");
    }

    @Test
    public void testDefaultImplementationReturnsSameInstance() {
        ServiceLoader<GuiceTelemetryRegistration> loader = ServiceLoader.load(GuiceTelemetryRegistration.class);
        GuiceTelemetryRegistration<?> impl = loader.findFirst().orElseThrow(() -> new AssertionError("No SPI implementation found"));
        OpenTelemetry base = GlobalOpenTelemetry.get();
        OpenTelemetry configured = impl.configure(base);
        assertSame(base, configured, "Default implementation should return the same OpenTelemetry instance");
    }
}
