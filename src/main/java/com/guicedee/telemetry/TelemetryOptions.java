package com.guicedee.telemetry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure Telemetry for GuicedEE.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface TelemetryOptions {
    /**
     * Whether telemetry is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    boolean enabled() default true;

    /**
     * The service name for telemetry.
     *
     * @return the service name.
     */
    String serviceName() default "GuicedEE-Application";

    /**
     * The endpoint for the OTLP exporter.
     *
     * @return the OTLP endpoint.
     */
    String otlpEndpoint() default "http://localhost:4317";

    /**
     * Whether to use in-memory exporters. Useful for testing.
     *
     * @return true if in-memory exporters should be used.
     */
    boolean useInMemoryExporters() default false;

    /**
     * Whether to configure logs via LogUtils.
     *
     * @return true if logs should be configured.
     */
    boolean configureLogs() default true;
}
