import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;

open module guiced.telemetry.test {
    requires transitive com.guicedee.telemetry;
    requires org.junit.jupiter.api;
    requires com.google.guice;
    requires com.guicedee.client;
    requires com.google.common;
    requires com.guicedee.services.opentelemetry;

    requires com.guicedee.health;

    exports com.guicedee.test;

    uses GuiceTelemetryRegistration;
}
