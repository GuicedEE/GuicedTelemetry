package com.guicedee.telemetry.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Marks a class or method to be traced via Guice AOP. When applied, an
 * interceptor will create an OpenTelemetry span around the method invocation.
 *
 * Usage:
 * - Annotate a type to trace all methods
 * - Annotate specific methods to trace individually
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@BindingAnnotation
public @interface Trace {
    /**
     * Optional span name override. If empty, a sensible default will be used
     * based on the declaring class and method name.
     */
    String value() default "";
}
