package com.enterprise.search.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Local development CORS policy for the Vite frontend.
 *
 * <p>Credentials are intentionally not enabled: the current application does
 * not use cookie-based authentication. This configuration can be replaced by
 * the security-layer CORS policy when authentication is introduced.</p>
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String VITE_DEFAULT_ORIGIN = "http://localhost:5173";
    private static final String VITE_LOCAL_ORIGIN = "http://localhost:5174";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(VITE_DEFAULT_ORIGIN, VITE_LOCAL_ORIGIN)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .maxAge(3600);
    }
}
