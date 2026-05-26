package ru.copperside.core.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "merchants-core.internal-admin")
public record InternalAdminSecurityProperties(String apiKey, @NotBlank String headerName) {
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
