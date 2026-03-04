package com.guicedee.telemetry.annotations;

import java.lang.annotation.*;

/**
 * Annotation to mark a parameter or a method return value to be included as a span attribute.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface SpanAttribute {
    /**
     * The name of the attribute. If empty, the parameter name or "return_value" will be used.
     *
     * @return the attribute name, or empty string for default
     */
    String value() default "";
}
