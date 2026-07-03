package com.orque.crm.config.query;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Extends Spring's default RequestMappingHandlerMapping to recognise the
 * HTTP QUERY method (RFC 10008) via the @QueryMapping annotation.
 *
 * When a method is annotated with @QueryMapping, a QueryMethodCondition is
 * attached as a custom condition. That condition only matches requests whose
 * HTTP method is "QUERY", while the base @RequestMapping (with no explicit
 * HTTP method set) would otherwise match anything.
 */
public class QueryRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        return AnnotatedElementUtils.hasAnnotation(method, QueryMapping.class)
                ? new QueryMethodCondition()
                : null;
    }

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        return AnnotatedElementUtils.hasAnnotation(handlerType, QueryMapping.class)
                ? new QueryMethodCondition()
                : null;
    }
}
