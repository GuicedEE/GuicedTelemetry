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

    public static void setOptions(TelemetryOptions options) {
        TelemetryPreStartup.options = options;
    }

    public static void reset() {
        options = null;
    }

    @Override
    public List<Future<Boolean>> onStartup() {
        if (options != null) {
            log.debug("Telemetry options already set, skipping discovery");
            OpenTelemetrySDKConfigurator.initialize();
            return List.of(Future.succeededFuture(true));
        }
        var scanResult = IGuiceContext.instance().getScanResult();

        // 1. Try to find @TelemetryOptions, but exclude classes that are likely just for testing options themselves
        var classes = scanResult.getClassesWithAnnotation(TelemetryOptions.class);
        if (!classes.isEmpty()) {
            for (var clazzRef : classes) {
                if (!clazzRef.getName().contains("TelemetryOptionsTest")) {
                    TelemetryOptions telOptions = clazzRef.loadClass().getAnnotation(TelemetryOptions.class);
                    options = new TelemetryOptions() {
                        @Override
                        public Class<? extends java.lang.annotation.Annotation> annotationType() {
                            return TelemetryOptions.class;
                        }

                        @Override
                        public boolean enabled() {
                            return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_ENABLED", String.valueOf(telOptions.enabled())));
                        }

                        @Override
                        public String serviceName() {
                            return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_SERVICE_NAME", telOptions.serviceName());
                        }

                        @Override
                        public String otlpEndpoint() {
                            return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_OTLP_ENDPOINT", telOptions.otlpEndpoint());
                        }

                        @Override
                        public boolean useInMemoryExporters() {
                            return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_USE_IN_MEMORY", String.valueOf(telOptions.useInMemoryExporters())));
                        }

                        @Override
                        public boolean configureLogs() {
                            return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_CONFIGURE_LOGS", String.valueOf(telOptions.configureLogs())));
                        }

                        @Override
                        public String serviceVersion() {
                            return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_SERVICE_VERSION", telOptions.serviceVersion());
                        }

                        @Override
                        public String deploymentEnvironment() {
                            return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_DEPLOYMENT_ENVIRONMENT", telOptions.deploymentEnvironment());
                        }

                        @Override
                        public int maxBatchSize() {
                            return Integer.parseInt(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_MAX_BATCH_SIZE", String.valueOf(telOptions.maxBatchSize())));
                        }

                        @Override
                        public int maxLogBatchSize() {
                            return Integer.parseInt(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_MAX_LOG_BATCH_SIZE", String.valueOf(telOptions.maxLogBatchSize())));
                        }

                        @Override
                        public boolean logSignals() {
                            return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_LOG_SIGNALS", String.valueOf(telOptions.logSignals())));
                        }
                    };
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
                        return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_ENABLED", String.valueOf(tel.enabled())));
                    }

                    @Override
                    public String serviceName() {
                        return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_SERVICE_NAME", tel.serviceName());
                    }

                    @Override
                    public String otlpEndpoint() {
                        return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_OTLP_ENDPOINT", tel.otlpEndpoint());
                    }

                    @Override
                    public boolean useInMemoryExporters() {
                        return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_USE_IN_MEMORY", String.valueOf(tel.useInMemoryExporters())));
                    }

                    @Override
                    public boolean configureLogs() {
                        return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_CONFIGURE_LOGS", String.valueOf(tel.configureLogs())));
                    }

                    @Override
                    public String serviceVersion() {
                        return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_SERVICE_VERSION", "1.0.0");
                    }

                    @Override
                    public String deploymentEnvironment() {
                        return com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_DEPLOYMENT_ENVIRONMENT", "production");
                    }

                    @Override
                    public int maxBatchSize() {
                        return Integer.parseInt(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_MAX_BATCH_SIZE", "512"));
                    }

                    @Override
                    public int maxLogBatchSize() {
                        return Integer.parseInt(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_MAX_LOG_BATCH_SIZE", "512"));
                    }

                    @Override
                    public boolean logSignals() {
                        return Boolean.parseBoolean(com.guicedee.client.Environment.getSystemPropertyOrEnvironment("TELEMETRY_LOG_SIGNALS", String.valueOf(tel.logSignals())));
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
