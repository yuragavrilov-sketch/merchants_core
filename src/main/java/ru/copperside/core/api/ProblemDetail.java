package ru.copperside.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * RFC 9457 ProblemDetail с платформенными полями: code, message, details, traceId.
 * См. ADR-0001 и contracts/openapi/_components.yaml.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemDetail(
        String type,
        String title,
        int status,
        String code,
        String message,
        Map<String, Object> details,
        String traceId
) {
}
