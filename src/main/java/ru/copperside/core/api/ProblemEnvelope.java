package ru.copperside.core.api;

import java.time.Instant;

public record ProblemEnvelope(
        ProblemDetail error,
        Instant timestamp
) {
    public static ProblemEnvelope of(ProblemDetail error) {
        return new ProblemEnvelope(error, Instant.now());
    }
}
