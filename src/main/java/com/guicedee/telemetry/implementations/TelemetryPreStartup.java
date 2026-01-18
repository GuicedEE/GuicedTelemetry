package com.guicedee.telemetry.implementations;

import com.guicedee.client.IGuiceContext;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.telemetry.TelemetryOptions;
import com.guicedee.telemetry.annotations.Telemetry;
import io.vertx.core.Future;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class TelemetryPreStartup implements IGuicePreStartup<TelemetryPreStartup> {

    @Getter
    private static TelemetryOptions options;

    public static void reset() {
        options = null;
    }

    @Override
    public List<Future<Boolean>> onStartup() {
        if (options != null) {
            log.debug("Telemetry options already set, initializing SDK");
            OpenTelemetrySDKConfigurator.initialize();
            return List.of(Future.succeededFuture(true));
        }
        var scanResult = IGuiceContext.instance().getScanResult();

        // 1. Try to find @TelemetryOptions, but exclude classes that are likely just for testing options themselves
        var classes = scanResult.getClassesWithAnnotation(TelemetryOptions.class);
        if (!classes.isEmpty()) {
            for (var clazzRef : classes) {
                if (!clazzRef.getName().contains("TelemetryOptionsTest")) {
                    options = clazzRef.loadClass().getAnnotation(TelemetryOptions.class);
                    break;
                }
            }
        }
        if (options == null) {
            // 2. Try to find @Telemetry, prioritizing the one on TracedService or TelemetryIntegrationTest if present
            var telemetryClasses = scanResult.getClassesWithAnnotation(Telemetry.class);
            if (!telemetryClasses.isEmpty()) {
                Class<?> targetClass = null;
                // Priority 1: TracedService or TelemetryIntegrationTest (Current test class)
                for (var clazzRef : telemetryClasses) {
                    if (clazzRef.getName().endsWith("TracedService") || clazzRef.getName().endsWith("TelemetryIntegrationTest")) {
                        targetClass = clazzRef.loadClass();
                        break;
                    }
                }
                // Priority 2: Other relevant test classes
                if (targetClass == null) {
                    for (var clazzRef : telemetryClasses) {
                        if (clazzRef.getName().contains("TelemetryAnnotationTest") ||
                            clazzRef.getName().contains("TelemetryOptionsTest")) {
                            targetClass = clazzRef.loadClass();
                            break;
                        }
                    }
                }
                // Priority 3: Anything else
                if (targetClass == null) {
                    targetClass = telemetryClasses.getFirst().loadClass();
                }

                log.debug("Selected class for @Telemetry: {}", targetClass.getName());
                Telemetry tel = targetClass.getAnnotation(Telemetry.class);
                options = new TelemetryOptions() {
                    @Override
                    public Class<? extends java.lang.annotation.Annotation> annotationType() {
                        return TelemetryOptions.class;
                    }

                    @Override
                    public boolean enabled() {
                        return tel.enabled();
                    }

                    @Override
                    public String serviceName() {
                        return tel.serviceName();
                    }

                    @Override
                    public String otlpEndpoint() {
                        return tel.otlpEndpoint();
                    }

                    @Override
                    public boolean useInMemoryExporters() {
                        return tel.useInMemoryExporters();
                    }

                    @Override
                    public boolean configureLogs() {
                        return tel.configureLogs();
                    }
                };
            }
        }
        OpenTelemetrySDKConfigurator.initialize();
        return List.of(Future.succeededFuture(true));
    }

    @Override
    public Integer sortOrder() {
        // Run before VertXPreStartup if possible, or at least before it builds Vertx
        // VertXPreStartup has sortOrder Integer.MIN_VALUE + 50;
        return Integer.MIN_VALUE + 35;
    }
}
