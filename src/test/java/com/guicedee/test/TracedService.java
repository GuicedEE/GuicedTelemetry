package com.guicedee.test;

import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Telemetry;
import com.guicedee.telemetry.annotations.Trace;
import io.smallrye.mutiny.Uni;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Telemetry(serviceName = "IntegrationTest", useInMemoryExporters = true)
public class TracedService{
    private static final Logger logger = LogManager.getLogger(TracedService.class);

    public static java.lang.invoke.MethodHandles.Lookup getModuleLookup() {
        return java.lang.invoke.MethodHandles.lookup();
    }

    @Trace("TestSpan")
    public void doWork() {
        logger.info("Doing some work in a traced method");
    }

    @Trace("AttributeSpan")
    @SpanAttribute("my_result")
    public String methodWithAttributes(@SpanAttribute("param1") String p1, @SpanAttribute int p2, Object complex) {
        return "Hello " + p1 + " " + p2;
    }

    @Trace("UniSpan")
    @SpanAttribute("uni_result")
    public Uni<String> uniMethod(@SpanAttribute("uni_param") String param) {
        return Uni.createFrom().item("Hello " + param);
    }

    @Trace("NestedOuter")
    public Uni<String> nestedOuter() {
        return Uni.createFrom().item("start")
                .chain(s -> nestedInner());
    }

    @Trace("NestedInner")
    public Uni<String> nestedInner() {
        return Uni.createFrom().item("inner");
    }
}
