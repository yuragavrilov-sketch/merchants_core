package ru.copperside.core.domain;

public enum MerchantConfigSortField {
    PARAMETER_NAME,
    PARAMETER_VALUE,
    DATE_BEGIN,
    DATE_END;

    public static MerchantConfigSortField from(String value) {
        if (value == null || value.isBlank()) {
            return PARAMETER_NAME;
        }

        return switch (value.trim().toLowerCase()) {
            case "parametername", "parameter_name", "name" -> PARAMETER_NAME;
            case "parametervalue", "parameter_value", "value" -> PARAMETER_VALUE;
            case "datebegin", "date_begin" -> DATE_BEGIN;
            case "dateend", "date_end" -> DATE_END;
            default -> throw new IllegalArgumentException("Unsupported sortBy for configurations: " + value);
        };
    }
}
