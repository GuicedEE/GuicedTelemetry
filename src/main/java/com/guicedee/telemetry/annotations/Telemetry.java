package com.guicedee.telemetry.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to configure Telemetry for GuicedEE.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Telemetry {
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
     * The base endpoint for the OTLP exporter.
     *
     * @return the OTLP base endpoint.
     */
    String otlpEndpoint() default "http://localhost:4318";

    /**
     * Optional dedicated full endpoint for trace (span) export.
     *
     * @return the OTLP traces endpoint, or empty to derive from the base.
     */
    String tracesEndpoint() default "";

    /**
     * Optional dedicated full endpoint for log record export.
     *
     * @return the OTLP logs endpoint, or empty to derive from the base.
     */
    String logsEndpoint() default "";

    /**
     * Whether to export logs over OTLP. Disable for traces-only backends (e.g. Tempo).
     *
     * @return true if OTLP log export should be configured.
     */
    boolean exportLogs() default true;

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

    /**
     * Whether to log the send and receipt of signals to the tracing server.
     *
     * @return true if signal logging is enabled.
     */
    boolean logSignals() default false;
}
