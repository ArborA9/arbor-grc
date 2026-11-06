package com.a9.arbor.grc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

/**
 * Hashed Vite assets + favicons from {@code classpath:/static/}.
 * Shell HTML is {@link SpaShellController} (ClassPathResource) for Azure nested-JAR reliability.
 */
@Configuration
public class SpaStaticConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        registry.addResourceHandler(
                        "/favicon.svg", "/favicon.ico", "/favicon-32.png", "/favicon-16.png", "/apple-touch-icon.png")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache());
    }
}
