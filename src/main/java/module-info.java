import com.guicedee.telemetry.implementations.GuiceTelemetryModuleInclusions;
import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;

module com.guicedee.telemetry {
    requires transitive com.guicedee.vertx;
    requires io.vertx.tracing.opentelemetry;

    provides com.guicedee.guicedinjection.interfaces.IGuiceScanModuleInclusions with GuiceTelemetryModuleInclusions;

    uses GuiceTelemetryRegistration;

}