package com.guicedee.telemetry.interceptors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * A simple Guice AOP interceptor that creates an OpenTelemetry span for methods
 * annotated with {@link Trace}, or for all methods in types annotated with {@link Trace}.
 */
public class TraceMethodInterceptor implements MethodInterceptor {

    private final Tracer tracer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TraceMethodInterceptor() {
        this(OpenTelemetrySDKConfigurator.getOpenTelemetry());
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
        try (var scope = span.makeCurrent()) {
            recordAttributes(span, method, invocation.getArguments());
            Object result = invocation.proceed();
            recordReturnAttribute(span, method, result);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage() == null ? "error" : t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }

    private void recordAttributes(Span span, Method method, Object[] args) {
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            SpanAttribute attr = parameters[i].getAnnotation(SpanAttribute.class);
            if (attr != null) {
                String name = attr.value().isBlank() ? parameters[i].getName() : attr.value();
                setAttribute(span, name, args[i]);
            }
        }
    }

    private void recordReturnAttribute(Span span, Method method, Object result) {
        SpanAttribute attr = method.getAnnotation(SpanAttribute.class);
        if (attr != null) {
            String name = attr.value().isBlank() ? "return_value" : attr.value();
            setAttribute(span, name, result);
        }
    }

    private void setAttribute(Span span, String name, Object value) {
        if (value == null) {
            span.setAttribute(name, "null");
            return;
        }

        if (value instanceof String) {
            span.setAttribute(name, (String) value);
        } else if (value instanceof Boolean) {
            span.setAttribute(name, (Boolean) value);
        } else if (value instanceof Long) {
            span.setAttribute(name, (Long) value);
        } else if (value instanceof Double) {
            span.setAttribute(name, (Double) value);
        } else if (value instanceof Integer) {
            span.setAttribute(name, ((Integer) value).longValue());
        } else if (value instanceof Float) {
            span.setAttribute(name, ((Float) value).doubleValue());
        } else {
            // Complex type - JSON
            try {
                span.setAttribute(name, objectMapper.writeValueAsString(value));
            } catch (Exception e) {
                span.setAttribute(name, value.toString());
            }
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
