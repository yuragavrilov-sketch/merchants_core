package ru.copperside.core.domain;

import java.util.Locale;

public record SearchTerm(String value) {
    public SearchTerm {
        value = value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase(Locale.ROOT);
    }

    public static SearchTerm of(String value) {
        return new SearchTerm(value);
    }

    public boolean isPresent() {
        return value != null;
    }

    public String likePattern() {
        return isPresent() ? "%" + value + "%" : null;
    }
}
