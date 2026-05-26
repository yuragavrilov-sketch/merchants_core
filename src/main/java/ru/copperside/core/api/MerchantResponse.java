package ru.copperside.core.api;

import ru.copperside.core.domain.Merchant;

public record MerchantResponse(
        Long mercId,
        String name,
        Long hierarchyId,
        String initiator,
        String circuit
) {
    public static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.mercId(),
                merchant.name(),
                merchant.hierarchyId(),
                merchant.initiator(),
                merchant.circuit()
        );
    }
}
