package ru.copperside.core.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MerchantRepository {
    List<Merchant> findAll(PageWindow page, SearchTerm search, SortOrder<MerchantSortField> sort);

    List<MerchantWithConfigLine> findAllWithActiveConfigLine(
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantSortField> sort
    );

    long countWithActiveConfigLine(SearchTerm search);

    Optional<Merchant> findById(Long mercId);

    boolean existsById(Long mercId);

    List<MerchantConfigEntry> findConfigByMerchantIdAt(
            Long mercId,
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantConfigSortField> sort
    );

    List<MerchantConfigEntry> findConfigHistoryByMerchantId(Long mercId);
}
