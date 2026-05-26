package ru.copperside.core.api;

import ru.copperside.core.domain.MerchantAdminLine;

import java.time.OffsetDateTime;

public record MerchantAdminLineResponse(
        Long mercId,
        String name,
        String status,
        String mcc,
        OffsetDateTime createdAt
) {
    public static MerchantAdminLineResponse from(MerchantAdminLine line) {
        return new MerchantAdminLineResponse(
                line.mercId(), line.name(), line.status(), line.mcc(), line.createdAt());
    }
}
