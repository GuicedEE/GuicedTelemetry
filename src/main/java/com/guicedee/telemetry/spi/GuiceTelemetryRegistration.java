package com.guicedee.telemetry.spi;

import com.guicedee.guicedinjection.interfaces.IDefaultService;
import io.opentelemetry.api.OpenTelemetry;

/**
 * SPI for telemetry registration hooks.
 * Implementations may customize or initialize the OpenTelemetry instance.
 */
public interface GuiceTelemetryRegistration<J extends GuiceTelemetryRegistration<J>> extends IDefaultService<J>
{
    /**
     * Allow implementations to customize or wrap the provided OpenTelemetry instance.
     * Return the instance to be used by the application (may be the same instance).
     */
    OpenTelemetry configure(OpenTelemetry openTelemetry);
}
