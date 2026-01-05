package com.alphaskyport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class PublicSecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    @Order(2) // Run after AdminSecurityConfig (Order 1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/shipments/**", "/api/quotes/**", "/api/public/**") // Define public/API scope
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/shipments/tracking/**").permitAll() // Explicitly allow tracking
                        .requestMatchers("/api/shipments/**").authenticated() // Secure other shipment endpoints
                        .anyRequest().authenticated());

        return http.build();
    }
}
