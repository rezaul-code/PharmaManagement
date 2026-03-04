package com.myspringboot.SpringBootApp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    // ── Interceptor registration ──────────────────────────────────────
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    // Landing + auth pages
                    "/",
                    "/index",
                    "/login",
                    "/login/**",
                    "/signup",
                    "/signup/**",

                    // Static resources
                    // NOTE: Spring Boot auto-serves /static/** but the
                    // interceptor still fires unless explicitly excluded.
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",

                    // Browser / Spring internals
                    "/favicon.ico",
                    "/error",
                    "/error/**"
                );
    }

    // ── Static resource handler ───────────────────────────────────────
    // Explicitly maps /css/**, /js/** etc. to src/main/resources/static/
    // This ensures DevTools hot-reload and the interceptor exclusions
    // both resolve to the same physical paths.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");

        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }
}