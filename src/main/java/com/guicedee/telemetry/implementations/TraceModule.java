package com.guicedee.telemetry.implementations;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.telemetry.annotations.Trace;
import com.guicedee.telemetry.interceptors.TraceMethodInterceptor;

/**
 * Guice module that binds the {@link TraceMethodInterceptor} to any class or
 * method annotated with {@link Trace}.
 */
public class TraceModule extends AbstractModule implements IGuiceModule<TraceModule> {
    @Override
    protected void configure() {
        TraceMethodInterceptor interceptor = new TraceMethodInterceptor();
        // Intercept any method where the TYPE is annotated with @Trace
        bindInterceptor(Matchers.annotatedWith(Trace.class), Matchers.any(), interceptor);
        // Intercept any method that is itself annotated with @Trace
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(Trace.class), interceptor);
    }
}
