package com.alphaskyport.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI alphaSkypOrtOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Alpha Skyport Freight Forwarding API")
                        .description("RESTful API for Alpha Skyport - A comprehensive freight forwarding platform")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Alpha Skyport Team")
                                .email("support@alphaskyport.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://alphaskyport.com/license")));
    }
}
