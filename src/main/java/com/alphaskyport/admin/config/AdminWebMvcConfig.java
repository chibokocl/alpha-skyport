package com.alphaskyport.admin.config;

import com.alphaskyport.admin.security.CurrentAdminArgumentResolver;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import org.springframework.lang.NonNull;

@Configuration
@RequiredArgsConstructor
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final CurrentAdminArgumentResolver currentAdminArgumentResolver;

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentAdminArgumentResolver);
    }
}
