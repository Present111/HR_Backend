package com.hrm.hrmapi.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class UploadConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory f = new MultipartConfigFactory();
        f.setMaxFileSize(DataSize.ofMegabytes(100));   // = spring.servlet.multipart.max-file-size
        f.setMaxRequestSize(DataSize.ofMegabytes(300));// = spring.servlet.multipart.max-request-size
        return f.createMultipartConfig();
    }
}
