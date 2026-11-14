package com.hotelportal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public AppConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Single source of "today" for date validation. Using the hotel's time zone keeps
     * "check-in cannot be in the past" consistent regardless of the server's own zone.
     */
    @Bean
    public Clock clock(@Value("${app.timezone:Asia/Kolkata}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
