package ru.copperside.core.domain;

import java.util.Objects;

public record SortOrder<T extends Enum<T>>(T field, SortDirection direction) {
    public SortOrder {
        field = Objects.requireNonNull(field, "field");
        direction = direction == null ? SortDirection.ASC : direction;
    }

    public static <T extends Enum<T>> SortOrder<T> of(T field, SortDirection direction) {
        return new SortOrder<>(field, direction);
    }
}
