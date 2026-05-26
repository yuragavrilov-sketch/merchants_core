package ru.copperside.core.infrastructure;

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
}
