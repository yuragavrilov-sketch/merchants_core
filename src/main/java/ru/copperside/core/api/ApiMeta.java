package ru.copperside.core.api;

public record ApiMeta(
        Integer limit,
        Integer offset,
        Integer count,
        String search,
        String sortBy,
        String sortDir,
        String at
) {
}
