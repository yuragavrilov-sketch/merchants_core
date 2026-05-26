package ru.copperside.core.application;

import ru.copperside.core.domain.Merchant;
import ru.copperside.core.domain.MerchantConfigEntry;
import ru.copperside.core.domain.MerchantConfigSnapshotSortField;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantRepository;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.MerchantWithConfigLine;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortDirection;
import ru.copperside.core.domain.SortOrder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MerchantServiceSnapshotTest {

    private final MerchantService service = new MerchantService(new StubMerchantRepository(), Clock.systemUTC());

    @Test
    void buildsConfigurationSnapshotsAcrossParameterBoundaries() {
        var snapshots = service.getConfigSnapshotsByMerchantId(
                1L,
                PageWindow.of(10, 0),
                SearchTerm.of(null),
                SortOrder.of(MerchantConfigSnapshotSortField.DATE_BEGIN, SortDirection.ASC)
        );

        assertThat(snapshots).hasSize(4);
        assertThat(snapshots.get(0).dateBegin()).isEqualTo(moment(2000));
        assertThat(snapshots.get(0).dateEnd()).isEqualTo(moment(2005));
        assertThat(snapshots.get(0).configuration()).containsOnlyKeys("MODE", "NAME");
        assertThat(snapshots.get(1).configuration()).containsEntry("INN", "1111111111");
        assertThat(snapshots.get(2).configuration()).containsEntry("NAME", "New Name");
        assertThat(snapshots.get(3).configuration()).containsOnlyKeys("INN", "NAME");
    }

    @Test
    void filtersSortsAndPagesSnapshotsInMemory() {
        var snapshots = service.getConfigSnapshotsByMerchantId(
                1L,
                PageWindow.of(1, 0),
                SearchTerm.of("new"),
                SortOrder.of(MerchantConfigSnapshotSortField.DATE_BEGIN, SortDirection.DESC)
        );

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().dateBegin()).isEqualTo(moment(2020));
        assertThat(snapshots.getFirst().configuration()).containsEntry("NAME", "New Name");
    }

    private static OffsetDateTime moment(int year) {
        return OffsetDateTime.of(year, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    private static class StubMerchantRepository implements MerchantRepository {
        @Override
        public List<Merchant> findAll(PageWindow page, SearchTerm search, SortOrder<MerchantSortField> sort) {
            return List.of();
        }

        @Override
        public List<MerchantWithConfigLine> findAllWithActiveConfigLine(
                OffsetDateTime atMoment,
                PageWindow page,
                SearchTerm search,
                SortOrder<MerchantSortField> sort
        ) {
            return List.of();
        }

        @Override
        public long countWithActiveConfigLine(SearchTerm search) {
            return 0L;
        }

        @Override
        public Optional<Merchant> findById(Long mercId) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long mercId) {
            return mercId == 1L;
        }

        @Override
        public List<MerchantConfigEntry> findConfigByMerchantIdAt(
                Long mercId,
                OffsetDateTime atMoment,
                PageWindow page,
                SearchTerm search,
                SortOrder<MerchantConfigSortField> sort
        ) {
            return List.of();
        }

        @Override
        public List<MerchantConfigEntry> findConfigHistoryByMerchantId(Long mercId) {
            return List.of(
                    new MerchantConfigEntry("NAME", "Old Name", moment(2000), moment(2010)),
                    new MerchantConfigEntry("NAME", "New Name", moment(2010), moment(2099)),
                    new MerchantConfigEntry("MODE", "A", moment(2000), moment(2020)),
                    new MerchantConfigEntry("INN", "1111111111", moment(2005), moment(2099))
            );
        }
    }
}
