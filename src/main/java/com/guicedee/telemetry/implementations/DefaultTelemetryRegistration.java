package com.guicedee.telemetry.implementations;

import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Default no-op Telemetry registration.
 *
 * This implementation allows GuicedEE environments to discover a baseline
 * telemetry configurator via ServiceLoader. It simply returns the provided
 * OpenTelemetry instance unchanged. Projects may provide their own
 * implementation with higher priority to customize the instance.
 */
public class DefaultTelemetryRegistration implements GuiceTelemetryRegistration<DefaultTelemetryRegistration> {

    @Override
    public OpenTelemetry configure(OpenTelemetry openTelemetry) {
        // No-op configuration by default; projects can override via their own SPI implementation
        return openTelemetry;
    }
}
