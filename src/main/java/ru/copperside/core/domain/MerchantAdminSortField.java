package ru.copperside.core.domain;

import java.util.Locale;

public enum MerchantAdminSortField {
    MERC_ID,
    NAME,
    STATUS,
    MCC,
    CREATED_AT;

    public static MerchantAdminSortField from(String value) {
        if (value == null || value.isBlank()) {
            return MERC_ID;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mercid", "merc_id", "id" -> MERC_ID;
            case "name" -> NAME;
            case "status" -> STATUS;
            case "mcc" -> MCC;
            case "createdat", "created_at" -> CREATED_AT;
            default -> throw new IllegalArgumentException("Unsupported sortBy for admin list: " + value);
        };
    }
}
