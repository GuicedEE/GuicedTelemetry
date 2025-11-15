package com.guicedee.telemetry.interceptors;

import com.guicedee.telemetry.annotations.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * A simple Guice AOP interceptor that creates an OpenTelemetry span for methods
 * annotated with {@link Trace}, or for all methods in types annotated with {@link Trace}.
 */
public class TraceMethodInterceptor implements MethodInterceptor {

    private final Tracer tracer;

    public TraceMethodInterceptor() {
        this(GlobalOpenTelemetry.get());
    }

    public TraceMethodInterceptor(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("com.guicedee.telemetry.trace");
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        String spanName = resolveSpanName(method);

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        try {
            return invocation.proceed();
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage() == null ? "error" : t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }

    private String resolveSpanName(Method method) {
        Trace trace = method.getAnnotation(Trace.class);
        if (trace == null) {
            trace = method.getDeclaringClass().getAnnotation(Trace.class);
        }
        if (trace != null && trace.value() != null && !trace.value().isBlank()) {
            return trace.value();
        }
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
}
