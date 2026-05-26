package ru.copperside.core.application;

import ru.copperside.core.domain.Merchant;
import ru.copperside.core.domain.MerchantConfigEntry;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantConfigSnapshot;
import ru.copperside.core.domain.MerchantConfigSnapshotSortField;
import ru.copperside.core.domain.MerchantRepository;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import ru.copperside.core.domain.MerchantWithConfigLine;
import ru.copperside.core.domain.SortDirection;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final Clock clock;

    public MerchantService(MerchantRepository merchantRepository, Clock clock) {
        this.merchantRepository = merchantRepository;
        this.clock = clock;
    }

    public List<Merchant> getAll(PageWindow page, SearchTerm search, SortOrder<MerchantSortField> sort) {
        return merchantRepository.findAll(page, search, sort);
    }

    public List<MerchantWithConfigLine> getAllWithActiveConfigLine(
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantSortField> sort
    ) {
        OffsetDateTime atMoment = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        return merchantRepository.findAllWithActiveConfigLine(atMoment, page, search, sort);
    }

    public long countWithActiveConfigLine(SearchTerm search) {
        return merchantRepository.countWithActiveConfigLine(search);
    }

    public Merchant getById(Long merchantId) {
        return merchantRepository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    public List<MerchantConfigEntry> getConfigByMerchantIdAt(
            Long merchantId,
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantConfigSortField> sort
    ) {
        List<MerchantConfigEntry> config = merchantRepository.findConfigByMerchantIdAt(
                merchantId,
                atMoment,
                page,
                search,
                sort
        );

        if (!config.isEmpty()) {
            return config;
        }

        if (!merchantRepository.existsById(merchantId)) {
            throw new MerchantNotFoundException(merchantId);
        }

        return config;
    }

    public List<MerchantConfigSnapshot> getConfigSnapshotsByMerchantId(
            Long merchantId,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantConfigSnapshotSortField> sort
    ) {
        List<MerchantConfigEntry> history = merchantRepository.findConfigHistoryByMerchantId(merchantId);
        if (history.isEmpty()) {
            if (!merchantRepository.existsById(merchantId)) {
                throw new MerchantNotFoundException(merchantId);
            }
            return List.of();
        }

        List<MerchantConfigSnapshot> snapshots = buildSnapshots(history);
        List<MerchantConfigSnapshot> filtered = applySnapshotSearch(snapshots, search);
        List<MerchantConfigSnapshot> sorted = applySnapshotSorting(filtered, sort);
        return sorted.stream().skip(page.offset()).limit(page.limit()).toList();
    }

    private List<MerchantConfigSnapshot> buildSnapshots(List<MerchantConfigEntry> history) {
        TreeSet<OffsetDateTime> boundaries = new TreeSet<>();
        NavigableMap<OffsetDateTime, List<MerchantConfigEntry>> starts = new TreeMap<>();
        NavigableMap<OffsetDateTime, List<MerchantConfigEntry>> ends = new TreeMap<>();

        for (MerchantConfigEntry entry : history) {
            boundaries.add(entry.dateBegin());
            boundaries.add(entry.dateEnd());
            starts.computeIfAbsent(entry.dateBegin(), ignored -> new ArrayList<>()).add(entry);
            ends.computeIfAbsent(entry.dateEnd(), ignored -> new ArrayList<>()).add(entry);
        }

        if (boundaries.size() < 2) {
            return List.of();
        }

        Map<String, NavigableMap<OffsetDateTime, List<MerchantConfigEntry>>> activeByParam = new LinkedHashMap<>();
        List<OffsetDateTime> timeline = boundaries.stream().toList();
        List<MerchantConfigSnapshot> snapshots = new ArrayList<>(timeline.size() - 1);

        for (int i = 0; i < timeline.size() - 1; i++) {
            OffsetDateTime point = timeline.get(i);
            OffsetDateTime nextPoint = timeline.get(i + 1);

            List<MerchantConfigEntry> endEntries = ends.get(point);
            if (endEntries != null) {
                for (MerchantConfigEntry entry : endEntries) {
                    removeActive(activeByParam, entry);
                }
            }

            List<MerchantConfigEntry> startEntries = starts.get(point);
            if (startEntries != null) {
                for (MerchantConfigEntry entry : startEntries) {
                    addActive(activeByParam, entry);
                }
            }

            if (nextPoint.isAfter(point)) {
                Map<String, String> config = snapshotConfig(activeByParam);
                if (!config.isEmpty()) {
                    snapshots.add(new MerchantConfigSnapshot(point, nextPoint, config));
                }
            }
        }

        return snapshots;
    }

    private void addActive(
            Map<String, NavigableMap<OffsetDateTime, List<MerchantConfigEntry>>> activeByParam,
            MerchantConfigEntry entry
    ) {
        activeByParam
                .computeIfAbsent(entry.parameterName(), ignored -> new TreeMap<>())
                .computeIfAbsent(entry.dateBegin(), ignored -> new ArrayList<>())
                .add(entry);
    }

    private void removeActive(
            Map<String, NavigableMap<OffsetDateTime, List<MerchantConfigEntry>>> activeByParam,
            MerchantConfigEntry entry
    ) {
        NavigableMap<OffsetDateTime, List<MerchantConfigEntry>> values = activeByParam.get(entry.parameterName());
        if (values == null) {
            return;
        }
        List<MerchantConfigEntry> entriesAtBegin = values.get(entry.dateBegin());
        if (entriesAtBegin != null) {
            entriesAtBegin.remove(entry);
            if (entriesAtBegin.isEmpty()) {
                values.remove(entry.dateBegin());
            }
        }
        if (values.isEmpty()) {
            activeByParam.remove(entry.parameterName());
        }
    }

    private Map<String, String> snapshotConfig(Map<String, NavigableMap<OffsetDateTime, List<MerchantConfigEntry>>> activeByParam) {
        LinkedHashMap<String, String> config = new LinkedHashMap<>();

        activeByParam.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<MerchantConfigEntry> effectiveBatch = entry.getValue().lastEntry().getValue();
                    MerchantConfigEntry effective = effectiveBatch.stream()
                            .max(Comparator.comparing(MerchantConfigEntry::dateEnd))
                            .orElseThrow();
                    config.put(entry.getKey(), effective.parameterValue());
                });

        return config;
    }

    private List<MerchantConfigSnapshot> applySnapshotSearch(List<MerchantConfigSnapshot> snapshots, SearchTerm search) {
        if (!search.isPresent()) {
            return snapshots;
        }

        String term = search.value();
        return snapshots.stream()
                .filter(snapshot -> snapshotMatches(snapshot, term))
                .toList();
    }

    private boolean snapshotMatches(MerchantConfigSnapshot snapshot, String term) {
        if (snapshot.dateBegin().toString().toLowerCase().contains(term)
                || snapshot.dateEnd().toString().toLowerCase().contains(term)) {
            return true;
        }

        return snapshot.configuration().entrySet().stream()
                .anyMatch(entry -> entry.getKey().toLowerCase().contains(term)
                        || String.valueOf(entry.getValue()).toLowerCase().contains(term));
    }

    private List<MerchantConfigSnapshot> applySnapshotSorting(
            List<MerchantConfigSnapshot> snapshots,
            SortOrder<MerchantConfigSnapshotSortField> sort
    ) {
        Comparator<MerchantConfigSnapshot> comparator = switch (sort.field()) {
            case DATE_BEGIN -> Comparator.comparing(MerchantConfigSnapshot::dateBegin);
            case DATE_END -> Comparator.comparing(MerchantConfigSnapshot::dateEnd);
        };

        if (sort.direction() == SortDirection.DESC) {
            comparator = comparator.reversed();
        }

        return snapshots.stream().sorted(comparator).toList();
    }
}
