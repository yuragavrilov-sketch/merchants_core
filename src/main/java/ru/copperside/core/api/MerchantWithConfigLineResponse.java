package ru.copperside.core.api;

import ru.copperside.core.domain.MerchantWithConfigLine;

import java.time.OffsetDateTime;
import java.util.Map;

public record MerchantWithConfigLineResponse(
        Long mercId,
        String name,
        Long hierarchyId,
        String initiator,
        String circuit,
        Map<String, String> configuration,
        OffsetDateTime activeSince
) {
    public static MerchantWithConfigLineResponse from(MerchantWithConfigLine merchant) {
        return new MerchantWithConfigLineResponse(
                merchant.mercId(),
                merchant.name(),
                merchant.hierarchyId(),
                merchant.initiator(),
                merchant.circuit(),
                merchant.configuration(),
                merchant.activeSince()
        );
    }
}
