package ru.copperside.core.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiMeta(
        Integer limit,
        Integer offset,
        Integer count,
        String search,
        String sortBy,
        String sortDir,
        String at,
        Long total,
        String status
) {
}
