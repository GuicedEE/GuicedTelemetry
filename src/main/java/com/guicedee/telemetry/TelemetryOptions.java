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
     * The base endpoint for the OTLP HTTP exporter.
     *
     * <p>This is treated as a <em>base</em> URL. The correct signal sub-path
     * ({@code /v1/traces} or {@code /v1/logs}) is appended automatically when it
     * is not already present, so {@code http://localhost:4318} and
     * {@code http://localhost:4318/v1/traces} both work. Per-signal overrides
     * ({@link #tracesEndpoint()} / {@link #logsEndpoint()}) take precedence.</p>
     *
     * @return the OTLP base endpoint.
     */
    String otlpEndpoint() default "http://localhost:4318";

    /**
     * Optional dedicated full endpoint for trace (span) export.
     *
     * <p>When set, this exact URL is used verbatim for spans (no sub-path is
     * appended). Use this to send traces to a traces backend such as Tempo or
     * Jaeger, e.g. {@code http://tempo:4318/v1/traces}. When blank the value is
     * derived from {@link #otlpEndpoint()} (or the
     * {@code OTEL_EXPORTER_OTLP_TRACES_ENDPOINT} environment variable).</p>
     *
     * @return the OTLP traces endpoint, or empty to derive from the base.
     */
    String tracesEndpoint() default "";

    /**
     * Optional dedicated full endpoint for log record export.
     *
     * <p>When set, this exact URL is used verbatim for logs (no sub-path is
     * appended). Tempo and Jaeger do <strong>not</strong> accept logs, so point
     * this at a logs-capable backend (Loki/OTel Collector), e.g.
     * {@code http://collector:4318/v1/logs}. When blank the value is derived
     * from {@link #otlpEndpoint()} (or the
     * {@code OTEL_EXPORTER_OTLP_LOGS_ENDPOINT} environment variable).</p>
     *
     * @return the OTLP logs endpoint, or empty to derive from the base.
     */
    String logsEndpoint() default "";

    /**
     * Whether to export logs over OTLP at all.
     *
     * <p>Set to {@code false} for traces-only backends (e.g. Tempo) that return
     * HTTP 404 on {@code /v1/logs}. When disabled, no log exporter, logger
     * provider, or OpenTelemetry Log4j2 appender is registered.</p>
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
     * The service version for telemetry.
     *
     * @return the service version.
     */
    String serviceVersion() default "1.0.0";

    /**
     * The environment for telemetry (e.g., production, staging, development).
     *
     * @return the deployment environment.
     */
    String deploymentEnvironment() default "production";

    /**
     * Maximum number of spans to batch before sending.
     *
     * @return the max batch size.
     */
    int maxBatchSize() default 512;

    /**
     * Maximum number of log records to batch before sending.
     *
     * @return the max log batch size.
     */
    int maxLogBatchSize() default 512;

    /**
     * Whether to log the send and receipt of signals to the tracing server.
     *
     * @return true if signal logging is enabled.
     */
    boolean logSignals() default false;
}
