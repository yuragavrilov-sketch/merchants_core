package ru.copperside.core.domain;

public enum MerchantSortField {
    MERC_ID,
    NAME,
    HIERARCHY_ID,
    INITIATOR,
    CIRCUIT;

    public static MerchantSortField from(String value) {
        if (value == null || value.isBlank()) {
            return MERC_ID;
        }

        return switch (value.trim().toLowerCase()) {
            case "mercid", "merc_id", "id" -> MERC_ID;
            case "name" -> NAME;
            case "hierarchyid", "hierarchy_id" -> HIERARCHY_ID;
            case "initiator" -> INITIATOR;
            case "circuit" -> CIRCUIT;
            default -> throw new IllegalArgumentException("Unsupported sortBy for merchants: " + value);
        };
    }
}
