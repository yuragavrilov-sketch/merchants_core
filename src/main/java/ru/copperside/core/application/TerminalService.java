package ru.copperside.core.application;

import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import ru.copperside.core.domain.TerminalPage;
import ru.copperside.core.domain.TerminalRepository;
import ru.copperside.core.domain.TerminalSortField;
import org.springframework.stereotype.Service;

@Service
public class TerminalService {

    private final TerminalRepository terminalRepository;

    public TerminalService(TerminalRepository terminalRepository) {
        this.terminalRepository = terminalRepository;
    }

    public TerminalPage getAll(PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort) {
        return terminalRepository.findAll(page, search, sort);
    }

    public TerminalPage getByMerchant(Long mercId, PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort) {
        return terminalRepository.findByMercId(mercId, page, search, sort);
    }
}
