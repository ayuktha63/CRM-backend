package com.orque.crm.config.query;

import org.springframework.core.annotation.AliasFor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.*;

/**
 * Marks a handler method as handling HTTP QUERY requests (RFC 10008).
 * QUERY is safe and idempotent like GET but accepts a structured request body,
 * enabling complex filtering without URI length limits.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@RequestMapping
public @interface QueryMapping {

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String[] value() default {};

    @AliasFor(annotation = RequestMapping.class, attribute = "path")
    String[] path() default {};

    @AliasFor(annotation = RequestMapping.class, attribute = "consumes")
    String[] consumes() default {"application/json"};

    @AliasFor(annotation = RequestMapping.class, attribute = "produces")
    String[] produces() default {"application/json"};

    @AliasFor(annotation = RequestMapping.class, attribute = "headers")
    String[] headers() default {};

    @AliasFor(annotation = RequestMapping.class, attribute = "params")
    String[] params() default {};
}
