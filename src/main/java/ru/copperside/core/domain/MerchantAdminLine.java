package ru.copperside.core.domain;

import java.time.OffsetDateTime;

public record MerchantAdminLine(
        Long mercId,
        String name,
        String status,
        String mcc,
        OffsetDateTime createdAt
) {
}
