package com.guicedee.telemetry.interceptors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Key;
import com.guicedee.client.IGuiceContext;
import com.guicedee.client.scopes.CallScoper;
import com.guicedee.telemetry.annotations.SpanAttribute;
import com.guicedee.telemetry.annotations.Trace;
import com.guicedee.telemetry.implementations.OpenTelemetrySDKConfigurator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A simple Guice AOP interceptor that creates an OpenTelemetry span for methods
 * annotated with {@link Trace}, or for all methods in types annotated with {@link Trace}.
 */
public class TraceMethodInterceptor implements MethodInterceptor {

    private static final Key<Span> SPAN_KEY = Key.get(Span.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Creates a new trace method interceptor.
     */
    public TraceMethodInterceptor() {
    }

    /**
     * Creates a new trace method interceptor with the given OpenTelemetry instance.
     *
     * @param openTelemetry the OpenTelemetry instance (currently unused, kept for API compatibility)
     */
    public TraceMethodInterceptor(OpenTelemetry openTelemetry) {
    }

    /**
     * Returns the tracer used for creating spans.
     *
     * @return the OpenTelemetry tracer
     */
    private Tracer getTracer() {
        return OpenTelemetrySDKConfigurator.getOpenTelemetry().getTracer("com.guicedee.telemetry.trace");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Tracer actualTracer = getTracer();
        Method method = invocation.getMethod();
        String spanName = resolveSpanName(method);

        CallScoper callScoper = IGuiceContext.get(CallScoper.class);
        Span parentSpan = null;
        if (callScoper.isStartedScope()) {
            parentSpan = (Span) callScoper.getValues().get(SPAN_KEY);
        }

        SpanBuilder spanBuilder = actualTracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL);

        if (parentSpan != null) {
            spanBuilder.setParent(Context.current().with(parentSpan));
        }

        Span span = spanBuilder.startSpan();

        if (callScoper.isStartedScope()) {
            callScoper.getValues().put(SPAN_KEY, span);
        }

        try (var scope = span.makeCurrent()) {
            recordAttributes(span, method, invocation.getArguments());
            Object result = invocation.proceed();
            if (result instanceof Uni<?>) {
                return wrapUni(span, method, (Uni<?>) result, callScoper, parentSpan);
            }
            recordReturnAttribute(span, method, result);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage() == null ? "error" : t.getMessage());
            span.end();
            if (callScoper.isStartedScope()) {
                if (parentSpan != null) {
                    callScoper.getValues().put(SPAN_KEY, parentSpan);
                } else {
                    callScoper.getValues().remove(SPAN_KEY);
                }
            }
            throw t;
        } finally {
            if (!Uni.class.isAssignableFrom(method.getReturnType())) {
                span.end();
                if (callScoper.isStartedScope()) {
                    if (parentSpan != null) {
                        callScoper.getValues().put(SPAN_KEY, parentSpan);
                    } else {
                        callScoper.getValues().remove(SPAN_KEY);
                    }
                }
            }
        }
    }

    /**
     * Wraps a {@link Uni} result so that the span ends when the Uni completes or fails.
     *
     * @param span       the current span
     * @param method     the intercepted method
     * @param uni        the Uni result to wrap
     * @param callScoper the current call scoper
     * @param parentSpan the parent span, or {@code null}
     * @return the wrapped Uni
     */
    private Uni<?> wrapUni(Span span, Method method, Uni<?> uni, CallScoper callScoper, Span parentSpan) {
        return uni.onItemOrFailure().invoke((item, failure) -> {
            try (var scope = span.makeCurrent()) {
                if (failure != null) {
                    span.recordException(failure);
                    span.setStatus(StatusCode.ERROR, failure.getMessage() == null ? "error" : failure.getMessage());
                } else {
                    recordReturnAttribute(span, method, item);
                }
            } finally {
                span.end();
                if (callScoper.isStartedScope()) {
                    if (parentSpan != null) {
                        callScoper.getValues().put(SPAN_KEY, parentSpan);
                    } else {
                        callScoper.getValues().remove(SPAN_KEY);
                    }
                }
            }
        });
    }

    /**
     * Records method parameters annotated with {@link SpanAttribute} on the span.
     *
     * @param span   the span to record attributes on
     * @param method the intercepted method
     * @param args   the method arguments
     */
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

    /**
     * Records the method return value as a span attribute if annotated with {@link SpanAttribute}.
     *
     * @param span   the span to record the attribute on
     * @param method the intercepted method
     * @param result the method return value
     */
    private void recordReturnAttribute(Span span, Method method, Object result) {
        SpanAttribute attr = method.getAnnotation(SpanAttribute.class);
        if (attr != null) {
            String name = attr.value().isBlank() ? "return_value" : attr.value();
            setAttribute(span, name, result);
        }
    }

    /**
     * Sets a typed attribute on the span, converting complex objects to JSON.
     *
     * @param span  the span to set the attribute on
     * @param name  the attribute name
     * @param value the attribute value
     */
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

    /**
     * Resolves the span name from the {@link Trace} annotation or falls back to class and method name.
     *
     * @param method the intercepted method
     * @return the span name
     */
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
