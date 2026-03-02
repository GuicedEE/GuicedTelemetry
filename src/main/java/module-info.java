import com.guicedee.client.services.config.IGuiceScanModuleInclusions;
import com.guicedee.telemetry.implementations.GuiceTelemetryModuleInclusions;
import com.guicedee.telemetry.implementations.DefaultTelemetryRegistration;
import com.guicedee.telemetry.implementations.TraceModule;
import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;
import com.guicedee.client.services.lifecycle.IGuiceModule;
import com.guicedee.client.services.lifecycle.IGuicePreStartup;
import com.guicedee.telemetry.implementations.TelemetryPreStartup;

module com.guicedee.telemetry {
    requires transitive com.guicedee.vertx;
    requires transitive com.guicedee.modules.services.opentelemetry;
    requires transitive com.guicedee.guicedinjection;
    requires transitive aopalliance;
    requires transitive org.apache.logging.log4j.core;
    requires static lombok;

    requires static com.guicedee.health;

    requires java.logging;

    exports com.guicedee.telemetry.annotations;
    exports com.guicedee.telemetry;
    exports com.guicedee.telemetry.spi;
    exports com.guicedee.telemetry.implementations;
    exports com.guicedee.telemetry.interceptors;

    provides IGuiceScanModuleInclusions with GuiceTelemetryModuleInclusions;
    provides com.guicedee.telemetry.spi.GuiceTelemetryRegistration with DefaultTelemetryRegistration;
    provides IGuiceModule with TraceModule;
    provides IGuicePreStartup with TelemetryPreStartup;

    opens com.guicedee.telemetry to com.google.guice;
    opens com.guicedee.telemetry.implementations to com.google.guice, com.guicedee.client;
    opens com.guicedee.telemetry.interceptors to com.google.guice;

    uses GuiceTelemetryRegistration;

}
