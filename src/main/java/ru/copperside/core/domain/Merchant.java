package ru.copperside.core.domain;

public record Merchant(
        Long mercId,
        String name,
        Long hierarchyId,
        String initiator,
        String circuit
) {
}
