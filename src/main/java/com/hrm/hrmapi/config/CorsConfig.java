package com.hrm.hrmapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();

        // ✅ Dùng allowedOrigins (không phải patterns) khi không có credentials
        cfg.setAllowedOrigins(List.of("http://localhost:5173"));

        // ✅ allowCredentials = false vì chỉ dùng Bearer token
        cfg.setAllowCredentials(false);

        // ✅ Cho phép các methods
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // ✅ Cho phép tất cả headers
        cfg.setAllowedHeaders(List.of("*"));

        // ✅ Expose headers để FE đọc được
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type"));

        // ✅ Cache preflight 1 giờ
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);

        return new CorsFilter(source);
    }
}