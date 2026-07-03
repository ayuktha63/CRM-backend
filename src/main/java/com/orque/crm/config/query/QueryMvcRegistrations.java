package com.orque.crm.config.query;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Registers QueryRequestMappingHandlerMapping as the primary handler mapping,
 * replacing Spring Boot's default. All standard annotations (@GetMapping,
 * @PostMapping, etc.) continue to work through the superclass.
 */
@Configuration
public class QueryMvcRegistrations implements WebMvcRegistrations {

    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new QueryRequestMappingHandlerMapping();
    }
}
