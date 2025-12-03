import com.guicedee.client.services.config.IGuiceScanModuleInclusions;
import com.guicedee.telemetry.implementations.GuiceTelemetryModuleInclusions;
import com.guicedee.telemetry.implementations.DefaultTelemetryRegistration;
import com.guicedee.telemetry.implementations.TraceModule;
import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import com.guicedee.client.services.lifecycle.IGuiceModule;

module com.guicedee.telemetry {
    requires transitive com.guicedee.vertx;
    requires transitive io.opentelemetry.api;
    requires transitive com.guicedee.guicedinjection;
    requires transitive aopalliance;

    exports com.guicedee.telemetry.annotations;
    exports com.guicedee.telemetry;

    provides IGuiceScanModuleInclusions with GuiceTelemetryModuleInclusions;
    provides com.guicedee.telemetry.spi.GuiceTelemetryRegistration with DefaultTelemetryRegistration;
    provides IGuiceModule with TraceModule;

    opens com.guicedee.telemetry to com.google.guice;
    opens com.guicedee.telemetry.implementations to com.google.guice;
    opens com.guicedee.telemetry.interceptors to com.google.guice;
    exports com.guicedee.telemetry.spi;

    uses GuiceTelemetryRegistration;
}
