package com.a9.arbor.grc.config;

import com.a9.arbor.grc.security.ModuleLicenseInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ModuleLicenseInterceptor moduleLicenseInterceptor;

    public WebMvcConfig(ModuleLicenseInterceptor moduleLicenseInterceptor) {
        this.moduleLicenseInterceptor = moduleLicenseInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleLicenseInterceptor).addPathPatterns("/api/**");
    }
}
