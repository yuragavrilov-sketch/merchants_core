package ru.copperside.core.domain;

import java.time.OffsetDateTime;

public record MerchantConfigEntry(
        String parameterName,
        String parameterValue,
        OffsetDateTime dateBegin,
        OffsetDateTime dateEnd
) {
}
