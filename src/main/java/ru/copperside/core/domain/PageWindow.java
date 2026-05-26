package ru.copperside.core.domain;

public record PageWindow(int limit, int offset) {
    public static final int MAX_LIMIT = 500;

    public PageWindow {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be less than or equal to " + MAX_LIMIT);
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be greater than or equal to 0");
        }
    }

    public static PageWindow of(int limit, int offset) {
        return new PageWindow(limit, offset);
    }
}
