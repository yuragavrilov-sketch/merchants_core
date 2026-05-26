package ru.copperside.core.api;

import ru.copperside.core.domain.MerchantConfigEntry;

import java.time.OffsetDateTime;

public record MerchantConfigEntryResponse(
        String parameterName,
        String parameterValue,
        OffsetDateTime dateBegin,
        OffsetDateTime dateEnd
) {
    public static MerchantConfigEntryResponse from(MerchantConfigEntry entry) {
        return new MerchantConfigEntryResponse(
                entry.parameterName(),
                entry.parameterValue(),
                entry.dateBegin(),
                entry.dateEnd()
        );
    }
}
