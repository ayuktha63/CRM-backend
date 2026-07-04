package com.orque.crm.config.query;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.Set;

/**
 * Enables HTTP QUERY (RFC 10008) end-to-end:
 *
 *  1. Spring Security's StrictHttpFirewall rejects non-standard HTTP methods with 400
 *     before the request ever reaches routing. Adding "QUERY" to the allowed set fixes this.
 *
 *  2. Tomcat only parses the request body for POST by default (parseBodyMethods).
 *     Adding "QUERY" lets @RequestBody work for QUERY handlers.
 */
@Configuration
public class QueryHttpConfig {

    @Bean
    public HttpFirewall queryAwareHttpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(Set.of(
                "DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT", "TRACE", "QUERY"
        ));
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer queryFirewallCustomizer(HttpFirewall queryAwareHttpFirewall) {
        return web -> web.httpFirewall(queryAwareHttpFirewall);
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatQueryBodyCustomizer() {
        return factory -> factory.addConnectorCustomizers(
                // parseBodyMethods is a field on Connector — use setProperty to configure it
                connector -> connector.setProperty("parseBodyMethods", "POST,QUERY")
        );
    }
}
