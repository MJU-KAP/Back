package com.example.NextPlan.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
public class CorsConfig {

    private String frontendOrigin;
    private Cors cors = new Cors();

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public void setFrontendOrigin(String frontendOrigin) { this.frontendOrigin = frontendOrigin; }
    public void setCors(Cors cors) { this.cors = cors; }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                List<String> origins = new ArrayList<>(cors.getAllowedOrigins());
                origins.add(frontendOrigin);
                registry.addMapping("/**")
                        .allowedOrigins(origins.toArray(String[]::new))
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization", "Set-Cookie")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
