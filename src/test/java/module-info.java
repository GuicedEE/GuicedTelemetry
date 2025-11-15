import com.guicedee.telemetry.spi.GuiceTelemetryRegistration;

open module guiced.telemetry.test {
    requires transitive com.guicedee.telemetry;
    requires io.opentelemetry.api;
    requires org.junit.jupiter.api;

    uses GuiceTelemetryRegistration;
}
