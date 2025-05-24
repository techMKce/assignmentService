package com.assignmentservice.assignmentservice.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

@Configuration

public class CorsConfig implements WebMvcConfigurer{
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/**") 
                .allowedOrigins("*") 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") 
                .allowedHeaders("*") 
                .allowCredentials(false);
    }
}
