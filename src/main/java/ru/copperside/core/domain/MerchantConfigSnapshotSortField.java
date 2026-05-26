package ru.copperside.core.domain;

public enum MerchantConfigSnapshotSortField {
    DATE_BEGIN,
    DATE_END;

    public static MerchantConfigSnapshotSortField from(String value) {
        if (value == null || value.isBlank()) {
            return DATE_BEGIN;
        }

        return switch (value.trim().toLowerCase()) {
            case "datebegin", "date_begin" -> DATE_BEGIN;
            case "dateend", "date_end" -> DATE_END;
            default -> throw new IllegalArgumentException("Unsupported sortBy for configuration history: " + value);
        };
    }
}
