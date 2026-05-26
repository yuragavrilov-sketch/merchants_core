package ru.copperside.core.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI merchantsCoreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Merchants Core API")
                        .description("Domain API for merchants and their configuration history")
                        .version("v1")
                        .contact(new Contact().name("Merchants Core Team")));
    }
}
