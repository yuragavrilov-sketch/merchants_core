package ru.copperside.core.infrastructure;

import ru.copperside.core.domain.TerminalPage;
import ru.copperside.core.domain.TerminalRepository;
import ru.copperside.core.domain.TerminalSortField;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortDirection;
import ru.copperside.core.domain.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OracleTerminalRepositoryIntegrationTest {

    @Autowired
    private TerminalRepository terminalRepository;

    private static final SortOrder<TerminalSortField> BY_MERC =
            SortOrder.of(TerminalSortField.MERC_ID, SortDirection.ASC);

    @Test
    void findAllReturnsAllRowsWithTotalAndMerchantName() {
        TerminalPage page = terminalRepository.findAll(PageWindow.of(100, 0), SearchTerm.of(null), BY_MERC);

        assertThat(page.total()).isEqualTo(4L);
        assertThat(page.lines()).hasSize(4);
        assertThat(page.lines().get(0).mercId()).isEqualTo(1L);
        assertThat(page.lines().get(0).merchantName()).isEqualTo("Alpha Shop");
        assertThat(page.lines().get(3).mercId()).isEqualTo(999L);
        assertThat(page.lines().get(3).merchantName()).isNull();
    }

    @Test
    void hasPasswordReflectsNonNullPassword() {
        TerminalPage page = terminalRepository.findByMercId(1L, PageWindow.of(100, 0), SearchTerm.of(null),
                SortOrder.of(TerminalSortField.MPS, SortDirection.ASC));

        assertThat(page.lines()).hasSize(2);
        assertThat(page.lines().get(0).mps()).isEqualTo("MASTER");
        assertThat(page.lines().get(0).hasPassword()).isFalse();
        assertThat(page.lines().get(0).is3ds()).isFalse();
        assertThat(page.lines().get(1).mps()).isEqualTo("VISA");
        assertThat(page.lines().get(1).hasPassword()).isTrue();
        assertThat(page.lines().get(1).is3ds()).isTrue();
    }

    @Test
    void findByMercIdScopesToMerchant() {
        assertThat(terminalRepository.findByMercId(2L, PageWindow.of(100, 0), SearchTerm.of(null), BY_MERC)
                .lines()).extracting(t -> t.mercId()).containsExactly(2L);
        assertThat(terminalRepository.findByMercId(999L, PageWindow.of(100, 0), SearchTerm.of(null), BY_MERC)
                .total()).isEqualTo(1L);
    }

    @Test
    void searchMatchesMpsTerminalIdAndMerchantName() {
        assertThat(terminalRepository.findAll(PageWindow.of(100, 0), SearchTerm.of("mir"), BY_MERC).lines())
                .extracting(t -> t.mercId()).containsExactly(2L);
        assertThat(terminalRepository.findAll(PageWindow.of(100, 0), SearchTerm.of("t3"), BY_MERC).lines())
                .extracting(t -> t.terminalId()).containsExactly("T3");
        assertThat(terminalRepository.findAll(PageWindow.of(100, 0), SearchTerm.of("alpha shop"), BY_MERC).lines())
                .extracting(t -> t.mercId()).containsExactly(1L, 1L);
    }

    @Test
    void paginationCarriesFullTotal() {
        TerminalPage page = terminalRepository.findAll(PageWindow.of(2, 0), SearchTerm.of(null), BY_MERC);
        assertThat(page.lines()).hasSize(2);
        assertThat(page.total()).isEqualTo(4L);
    }

    @Test
    void emptyResultReportsZeroTotal() {
        TerminalPage page = terminalRepository.findAll(PageWindow.of(100, 0), SearchTerm.of("zzz-no-match"), BY_MERC);
        assertThat(page.lines()).isEmpty();
        assertThat(page.total()).isZero();
    }
}
