package com.example.urlshortener.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI urlShortenerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("URL Shortener API")
                .description("Encurtador de URLs com métricas, cache Redis e rate limiting por IP")
                .version("1.0.0")
                .contact(new Contact().name("Example").email("dev@example.com"))
                .license(new License().name("MIT")));
    }
}
