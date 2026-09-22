package com.guicedee.telemetry;

import com.guicedee.vertx.spi.Verticle;
import com.guicedee.telemetry.annotations.Trace;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;

/**
 * Public GuicedEE telemetry facade for application and infrastructure modules.
 *
 * <p>Use this facade at framework boundaries such as HTTP clients, HTTP
 * servers, messaging, service discovery, and persistence adapters. The
 * {@link Trace} annotation remains the convenient choice for Guice AOP method
 * tracing.</p>
 */
@Verticle(value = "guiced-telemetry")
public class GuicedTelemetry {

    private static final io.opentelemetry.context.propagation.TextMapSetter<MultiMap> SETTER =
            (carrier, key, value) -> carrier.set(key, value);
    private static final io.opentelemetry.context.propagation.TextMapSetter<java.util.Map<String, String>> MAP_SETTER =
            (carrier, key, value) -> carrier.put(key, value);
    private static final io.opentelemetry.context.propagation.TextMapGetter<MultiMap> GETTER =
            new io.opentelemetry.context.propagation.TextMapGetter<>() {
                @Override
                public String get(MultiMap carrier, String key) {
                    return carrier == null ? null : carrier.get(key);
                }

                @Override
                public java.util.Collection<String> keys(MultiMap carrier) {
                    return carrier == null ? java.util.List.of() : carrier.names();
                }
            };

    /**
     * Creates a new GuicedTelemetry instance.
     */
    public GuicedTelemetry() {
    }

    /** Returns the configured global OpenTelemetry facade. */
    public static OpenTelemetry openTelemetry() {
        return GlobalOpenTelemetry.get();
    }

    /** Returns a tracer for a stable library or module instrumentation name. */
    public static Tracer tracer(String instrumentationName) {
        return openTelemetry().getTracer(instrumentationName);
    }

    /** Returns the current span, or an invalid span when no span is active. */
    public static Span currentSpan() {
        return Span.current();
    }

    /** Starts an internal span using the current propagated context. */
    public static Span startSpan(String name) {
        return tracer("com.guicedee.telemetry").spanBuilder(name)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
    }

    /** Starts a client span using the current propagated context. */
    public static Span startClientSpan(String method, String url) {
        return tracer("com.guicedee.telemetry.http")
                .spanBuilder(method + " " + url)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("http.request.method", method)
                .setAttribute("url.full", url)
                .startSpan();
    }

    /** Starts a server span with the parent extracted from incoming headers. */
    public static Span startServerSpan(String method, String route, MultiMap headers) {
        Context parent = extract(headers);
        return tracer("com.guicedee.telemetry.http")
                .spanBuilder(method + " " + route)
                .setParent(parent)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.request.method", method)
                .setAttribute("http.route", route)
                .startSpan();
    }

    /** Injects the current W3C trace context into a Vert.x header carrier. */
    public static void inject(MultiMap headers) {
        if (headers != null) {
            openTelemetry().getPropagators().getTextMapPropagator()
                    .inject(Context.current(), headers, SETTER);
        }
    }

    /** Injects the current W3C trace context into a simple string header map. */
    public static void inject(java.util.Map<String, String> headers) {
        if (headers != null) {
            openTelemetry().getPropagators().getTextMapPropagator()
                    .inject(Context.current(), headers, MAP_SETTER);
        }
    }

    /** Extracts W3C trace context from a Vert.x header carrier. */
    public static Context extract(MultiMap headers) {
        return openTelemetry().getPropagators().getTextMapPropagator()
                .extract(Context.current(), headers, GETTER);
    }

    /** Makes a span current for a bounded synchronous operation. */
    public static Scope makeCurrent(Span span) {
        return span == null ? Scope.noop() : span.makeCurrent();
    }

    /** Completes a span and records failures from a synchronous operation. */
    public static void end(Span span, Throwable failure) {
        if (span == null) return;
        if (failure != null) {
            span.recordException(failure);
            span.setStatus(StatusCode.ERROR, failure.getMessage() == null ? "error" : failure.getMessage());
        }
        span.end();
    }

    /** Keeps a span open until a Mutiny operation completes or fails. */
    public static <T> Uni<T> finish(Span span, Uni<T> operation) {
        if (span == null || operation == null) return operation;
        return withSpan(span, operation).onItemOrFailure().invoke((item, failure) -> {
            end(span, failure);
        });
    }

    /**
     * Keeps a span current for the complete subscription lifetime of a Uni.
     * This is required when a REST resource returns a cold Uni whose work,
     * including a downstream RestClient call, starts only on subscription.
     */
    public static <T> Uni<T> withSpan(Span span, Uni<T> operation) {
        if (span == null || operation == null) return operation;
        return Uni.createFrom().emitter(emitter -> {
            Scope scope = span.makeCurrent();
            emitter.onTermination(scope::close);
            try (Scope ignored = span.makeCurrent()) {
                operation.subscribe().with(emitter::complete, emitter::fail);
            }
        });
    }

    /** Keeps a span current while a Vert.x Future completes. */
    public static <T> Future<T> withSpan(Span span, Future<T> operation) {
        if (span == null || operation == null) return operation;
        return Future.future(promise -> {
            try (Scope ignored = span.makeCurrent()) {
                operation.onComplete(result -> {
                    try (Scope callback = span.makeCurrent()) {
                        if (result.succeeded()) promise.complete(result.result());
                        else promise.fail(result.cause());
                    }
                });
            }
        });
    }

    /** Keeps a span open until a Vert.x Future completes or fails. */
    public static <T> Future<T> finish(Span span, Future<T> operation) {
        if (span == null || operation == null) return operation;
        return withSpan(span, operation).onComplete(result -> end(span, result.failed() ? result.cause() : null));
    }
}
