package ru.copperside.core.domain;

public interface TerminalRepository {

    TerminalPage findAll(PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort);

    TerminalPage findByMercId(Long mercId, PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort);
}
