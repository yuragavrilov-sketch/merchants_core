package ru.copperside.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Success-конверт API по ADR-0001. Для ошибок используется {@link ProblemEnvelope}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        ApiMeta meta,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Object error,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(T data, ApiMeta meta) {
        return new ApiResponse<>(data, meta, null, Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, null, Instant.now());
    }
}
