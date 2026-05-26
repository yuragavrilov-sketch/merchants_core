package ru.copperside.core.domain;

import java.time.OffsetDateTime;
import java.util.Map;

public record MerchantWithConfigLine(
        Long mercId,
        String name,
        Long hierarchyId,
        String initiator,
        String circuit,
        Map<String, String> configuration,
        OffsetDateTime activeSince
) {
}
