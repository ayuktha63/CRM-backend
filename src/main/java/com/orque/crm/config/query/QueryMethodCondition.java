package com.orque.crm.config.query;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom Spring MVC request condition that matches only HTTP QUERY requests (RFC 10008).
 * Works in tandem with @QueryMapping: the base @RequestMapping has no methods array
 * (matches all), and this condition narrows it down to only the QUERY method.
 */
public class QueryMethodCondition extends AbstractRequestCondition<QueryMethodCondition> {

    @Override
    protected Collection<?> getContent() {
        return Collections.singleton("QUERY");
    }

    @Override
    protected String getToStringInfix() {
        return " || ";
    }

    @Override
    public QueryMethodCondition combine(QueryMethodCondition other) {
        return this;
    }

    @Override
    public QueryMethodCondition getMatchingCondition(HttpServletRequest request) {
        return "QUERY".equalsIgnoreCase(request.getMethod()) ? this : null;
    }

    @Override
    public int compareTo(QueryMethodCondition other, HttpServletRequest request) {
        return 0;
    }
}
