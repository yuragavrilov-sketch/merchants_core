package ru.copperside.core.domain;

import java.time.OffsetDateTime;
import java.util.Map;

public record MerchantConfigSnapshot(
        OffsetDateTime dateBegin,
        OffsetDateTime dateEnd,
        Map<String, String> configuration
) {
}
