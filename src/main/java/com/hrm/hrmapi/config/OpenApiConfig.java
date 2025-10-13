package com.hrm.hrmapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hrmOpenAPI() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authorization");

        return new OpenAPI()
                .info(new Info()
                        .title("HRM API")
                        .description("""
                Sprint 1 endpoints: Auth, Users, Departments, Employees, Documents.
                Dùng nút Authorize (Bearer) để gắn JWT.
                """)
                        .version("v1.0")
                        .contact(new Contact().name("HR Team")))
                // Khai báo scheme và set làm mặc định cho tất cả endpoints
                .schemaRequirement("bearerAuth", bearer)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    // Nhóm tài liệu theo package (tùy chọn)
    @Bean
    public GroupedOpenApi hrmGroup() {
        return GroupedOpenApi.builder()
                .group("hrm")
                .packagesToScan("com.hrm.hrmapi.web")
                .build();
    }
}
