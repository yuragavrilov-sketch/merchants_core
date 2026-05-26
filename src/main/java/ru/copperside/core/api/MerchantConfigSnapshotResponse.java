package ru.copperside.core.api;

import ru.copperside.core.domain.MerchantConfigSnapshot;

import java.time.OffsetDateTime;
import java.util.Map;

public record MerchantConfigSnapshotResponse(
        OffsetDateTime dateBegin,
        OffsetDateTime dateEnd,
        Map<String, String> configuration
) {
    public static MerchantConfigSnapshotResponse from(MerchantConfigSnapshot snapshot) {
        return new MerchantConfigSnapshotResponse(
                snapshot.dateBegin(),
                snapshot.dateEnd(),
                snapshot.configuration()
        );
    }
}
