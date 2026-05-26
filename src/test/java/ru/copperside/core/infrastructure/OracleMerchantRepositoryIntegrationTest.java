package ru.copperside.core.infrastructure;

import ru.copperside.core.domain.MerchantAdminPage;
import ru.copperside.core.domain.MerchantAdminSortField;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantRepository;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import ru.copperside.core.domain.SortDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OracleMerchantRepositoryIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    private static final OffsetDateTime AT = OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void findAllSupportsPaginationSearchAndSorting() {
        var merchants = merchantRepository.findAll(
                PageWindow.of(2, 0),
                SearchTerm.of("market"),
                SortOrder.of(MerchantSortField.NAME, SortDirection.ASC)
        );

        assertThat(merchants).hasSize(1);
        assertThat(merchants.getFirst().name()).isEqualTo("Beta Market");
    }

    @Test
    void findAllWithActiveConfigLineReturnsFlattenedConfig() {
        var merchants = merchantRepository.findAllWithActiveConfigLine(
                OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                PageWindow.of(10, 0),
                SearchTerm.of("1111111111"),
                SortOrder.of(MerchantSortField.MERC_ID, SortDirection.ASC)
        );

        assertThat(merchants).hasSize(1);
        assertThat(merchants.getFirst().mercId()).isEqualTo(1L);
        assertThat(merchants.getFirst().configuration()).containsEntry("INN", "1111111111");
        assertThat(merchants.getFirst().activeSince())
                .isEqualTo(OffsetDateTime.of(1999, 12, 31, 21, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void findAllWithActiveConfigLineReturnsNullActiveSinceWhenNoActiveConfig() {
        var merchants = merchantRepository.findAllWithActiveConfigLine(
                OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                PageWindow.of(10, 0),
                SearchTerm.of("gamma"),
                SortOrder.of(MerchantSortField.MERC_ID, SortDirection.ASC)
        );

        assertThat(merchants).hasSize(1);
        assertThat(merchants.getFirst().mercId()).isEqualTo(3L);
        assertThat(merchants.getFirst().configuration()).isEmpty();
        assertThat(merchants.getFirst().activeSince()).isNull();
    }

    @Test
    void countWithActiveConfigLineCountsAllMerchantsWhenNoSearch() {
        long total = merchantRepository.countWithActiveConfigLine(SearchTerm.of(null));
        assertThat(total).isEqualTo(6L);
    }

    @Test
    void countWithActiveConfigLineAppliesMerchantSearch() {
        assertThat(merchantRepository.countWithActiveConfigLine(SearchTerm.of("market"))).isEqualTo(1L);
        assertThat(merchantRepository.countWithActiveConfigLine(SearchTerm.of("zzz-no-match"))).isEqualTo(0L);
    }

    @Test
    void findConfigByMerchantIdAtSupportsPaginationSearchAndSorting() {
        var configs = merchantRepository.findConfigByMerchantIdAt(
                1L,
                OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                PageWindow.of(10, 0),
                SearchTerm.of("name"),
                SortOrder.of(MerchantConfigSortField.PARAMETER_NAME, SortDirection.ASC)
        );

        assertThat(configs).hasSize(1);
        assertThat(configs.getFirst().parameterName()).isEqualTo("NAME");
        assertThat(configs.getFirst().parameterValue()).isEqualTo("Alpha Shop LLC");
    }

    @Test
    void adminProjectionDerivesStatusMccCreatedAt() {
        MerchantAdminPage page = merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of(null), null,
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC));

        assertThat(page.total()).isEqualTo(6L);
        assertThat(page.lines()).extracting(l -> l.mercId() + ":" + l.status() + ":" + l.mcc())
                .containsExactly(
                        "1:active:0000",
                        "2:active:0000",
                        "3:blocked:0000",
                        "4:active:5411",
                        "5:suspended:4111",
                        "6:blocked:0000");
        assertThat(page.lines().get(3).createdAt())
                .isEqualTo(OffsetDateTime.of(2017, 12, 31, 21, 0, 0, 0, ZoneOffset.UTC));
        assertThat(page.lines().get(2).createdAt()).isNull();
        assertThat(page.lines().get(0).createdAt())
                .isEqualTo(OffsetDateTime.of(1999, 12, 31, 21, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void adminProjectionFiltersByStatus() {
        MerchantAdminPage active = merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of(null), "active",
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC));
        assertThat(active.total()).isEqualTo(3L);
        assertThat(active.lines()).extracting(l -> l.mercId()).containsExactly(1L, 2L, 4L);

        MerchantAdminPage blocked = merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of(null), "blocked",
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC));
        assertThat(blocked.total()).isEqualTo(2L);
        assertThat(blocked.lines()).extracting(l -> l.mercId()).containsExactly(3L, 6L);
    }

    @Test
    void adminProjectionSearchMatchesFormattedIdNameStatusMcc() {
        assertThat(merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of("mrc-00005"), null,
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC)).lines())
                .extracting(l -> l.mercId()).containsExactly(5L);
        assertThat(merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of("5411"), null,
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC)).lines())
                .extracting(l -> l.mercId()).containsExactly(4L);
        assertThat(merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of("suspended"), null,
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC)).lines())
                .extracting(l -> l.mercId()).containsExactly(5L);
    }

    @Test
    void adminProjectionSortsAndPaginatesWithTotal() {
        MerchantAdminPage byMccDesc = merchantRepository.findAdminProjection(
                AT, PageWindow.of(2, 0), SearchTerm.of(null), null,
                SortOrder.of(MerchantAdminSortField.MCC, SortDirection.DESC));
        assertThat(byMccDesc.total()).isEqualTo(6L);
        assertThat(byMccDesc.lines()).hasSize(2);
        assertThat(byMccDesc.lines().get(0).mcc()).isEqualTo("5411");
    }

    @Test
    void adminProjectionEmptyPageReportsZeroTotal() {
        MerchantAdminPage none = merchantRepository.findAdminProjection(
                AT, PageWindow.of(100, 0), SearchTerm.of("zzz-no-such-merchant"), null,
                SortOrder.of(MerchantAdminSortField.MERC_ID, SortDirection.ASC));
        assertThat(none.lines()).isEmpty();
        assertThat(none.total()).isZero();
    }
}
