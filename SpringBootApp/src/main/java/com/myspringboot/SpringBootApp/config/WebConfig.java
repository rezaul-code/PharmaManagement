package com.myspringboot.SpringBootApp.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private TenantInterceptor tenantInterceptor;  // ← NEW

    // ── Interceptor registration ──────────────────────────────────────
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1️⃣  Auth guard — unchanged
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/", "/index",
                    "/login", "/login/**",
                    "/signup", "/signup/**",
                    "/css/**", "/js/**", "/images/**", "/fonts/**",
                    "/favicon.ico", "/error", "/error/**"
                )
                .order(1);

        // 2️⃣  Tenant context loader — runs on EVERY request (including public
        //     ones) so TenantContext is always initialised and safely cleared.
        //     It is harmless on unauthenticated requests because it falls back
        //     to DEFAULT_PHARMACY_ID and gets cleared in afterCompletion().
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/**")
                .order(2);
    }

    // ── Static resource handler ───────────────────────────────────────
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        // Serve uploaded files (logos etc.) from local filesystem
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
    
    @Autowired
    private LocalDateConverter localDateConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(localDateConverter);
    }
}