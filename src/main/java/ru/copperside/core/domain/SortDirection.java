package ru.copperside.core.domain;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return SortDirection.valueOf(value.trim().toUpperCase());
    }
}
