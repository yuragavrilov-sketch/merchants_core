package ru.copperside.core.infrastructure;

import ru.copperside.core.domain.Merchant;
import ru.copperside.core.domain.MerchantConfigEntry;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantRepository;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.MerchantWithConfigLine;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class OracleMerchantRepository implements MerchantRepository {

    private static final String MERCHANTS_SELECT = """
            SELECT m.MERCID, m.NAME, m.HIERARCHYID, m.INITIATOR, m.CIRCUIT
            FROM "AP#MERCHANTS" m
            """;

    private static final String CONFIG_AT_MOMENT_SELECT = """
            SELECT PARAMETERNAME, PARAMETERVALUE, DATEBEGIN, DATEEND
            FROM MERC_CONFIG
            WHERE MERCID = :mercId
              AND :atMoment >= DATEBEGIN
              AND :atMoment < DATEEND
            """;

    private static final String CONFIG_HISTORY_SELECT = """
            SELECT PARAMETERNAME, PARAMETERVALUE, DATEBEGIN, DATEEND
            FROM MERC_CONFIG
            WHERE MERCID = :mercId
            ORDER BY DATEBEGIN, DATEEND, PARAMETERNAME
            """;

    private static final String ACTIVE_LINE_MERCHANT_PAGE_CTE = """
            WITH merchant_page AS (
                SELECT m.MERCID, m.NAME, m.HIERARCHYID, m.INITIATOR, m.CIRCUIT
                FROM "AP#MERCHANTS" m
            """;

    private static final String ACTIVE_LINE_SELECT = """
            SELECT
                mp.MERCID,
                mp.NAME,
                mp.HIERARCHYID,
                mp.INITIATOR,
                mp.CIRCUIT,
                c.PARAMETERNAME,
                c.PARAMETERVALUE
            FROM merchant_page mp
            LEFT JOIN MERC_CONFIG c
                ON c.MERCID = mp.MERCID
               AND :atMoment >= c.DATEBEGIN
               AND :atMoment < c.DATEEND
            ORDER BY mp.MERCID, c.PARAMETERNAME
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleMerchantRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Merchant> findAll(PageWindow page, SearchTerm search, SortOrder<MerchantSortField> sort) {
        String sql = MERCHANTS_SELECT
                + merchantSearchWhereClause(search)
                + " ORDER BY " + merchantSortColumn(sort.field()) + " " + sort.direction().name()
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", page.limit())
                .addValue("offset", page.offset());
        addSearchParam(params, search);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new Merchant(
                rs.getLong("MERCID"),
                rs.getString("NAME"),
                readNullableLong(rs, "HIERARCHYID"),
                rs.getString("INITIATOR"),
                rs.getString("CIRCUIT")
        ));
    }

    @Override
    public List<MerchantWithConfigLine> findAllWithActiveConfigLine(
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantSortField> sort
    ) {
        String cte = ACTIVE_LINE_MERCHANT_PAGE_CTE
                + activeLineSearchWhereClause(search)
                + " ORDER BY " + merchantSortColumn(sort.field()) + " " + sort.direction().name()
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY) ";

        String sql = cte + ACTIVE_LINE_SELECT;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atMoment", Timestamp.from(atMoment.toInstant()))
                .addValue("limit", page.limit())
                .addValue("offset", page.offset());
        addSearchParam(params, search);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, params);
        return aggregateConfigLine(rows);
    }

    @Override
    public Optional<Merchant> findById(Long mercId) {
        String sql = MERCHANTS_SELECT + " WHERE m.MERCID = :mercId";
        List<Merchant> merchants = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("mercId", mercId),
                (rs, rowNum) -> new Merchant(
                        rs.getLong("MERCID"),
                        rs.getString("NAME"),
                        readNullableLong(rs, "HIERARCHYID"),
                        rs.getString("INITIATOR"),
                        rs.getString("CIRCUIT")
                )
        );

        return merchants.stream().findFirst();
    }

    @Override
    public boolean existsById(Long mercId) {
        String sql = "SELECT CASE WHEN COUNT(1) > 0 THEN 1 ELSE 0 END AS EXISTS_FLAG FROM \"AP#MERCHANTS\" WHERE MERCID = :mercId";
        Integer value = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource("mercId", mercId), Integer.class);
        return value != null && value == 1;
    }

    @Override
    public List<MerchantConfigEntry> findConfigByMerchantIdAt(
            Long mercId,
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            SortOrder<MerchantConfigSortField> sort
    ) {
        String sql = CONFIG_AT_MOMENT_SELECT
                + configSearchWhereClause(search)
                + " ORDER BY " + configSortColumn(sort.field()) + " " + sort.direction().name()
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mercId", mercId)
                .addValue("atMoment", Timestamp.from(atMoment.toInstant()))
                .addValue("limit", page.limit())
                .addValue("offset", page.offset());
        addSearchParam(params, search);

        return jdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> new MerchantConfigEntry(
                        rs.getString("PARAMETERNAME"),
                        rs.getString("PARAMETERVALUE"),
                        toUtc(rs.getTimestamp("DATEBEGIN")),
                        toUtc(rs.getTimestamp("DATEEND"))
                )
        );
    }

    @Override
    public List<MerchantConfigEntry> findConfigHistoryByMerchantId(Long mercId) {
        return jdbcTemplate.query(
                CONFIG_HISTORY_SELECT,
                new MapSqlParameterSource("mercId", mercId),
                (rs, rowNum) -> new MerchantConfigEntry(
                        rs.getString("PARAMETERNAME"),
                        rs.getString("PARAMETERVALUE"),
                        toUtc(rs.getTimestamp("DATEBEGIN")),
                        toUtc(rs.getTimestamp("DATEEND"))
                )
        );
    }

    private List<MerchantWithConfigLine> aggregateConfigLine(List<Map<String, Object>> rows) {
        Map<Long, MerchantAccumulator> grouped = new LinkedHashMap<>();

        for (Map<String, Object> row : rows) {
            Long mercId = toLong(row.get("MERCID"));
            MerchantAccumulator acc = grouped.computeIfAbsent(mercId, key -> new MerchantAccumulator(
                    key,
                    (String) row.get("NAME"),
                    toLong(row.get("HIERARCHYID")),
                    (String) row.get("INITIATOR"),
                    (String) row.get("CIRCUIT")
            ));

            String parameterName = (String) row.get("PARAMETERNAME");
            String parameterValue = (String) row.get("PARAMETERVALUE");
            if (parameterName != null && parameterValue != null) {
                acc.configuration.put(parameterName, parameterValue);
            }
        }

        List<MerchantWithConfigLine> result = new ArrayList<>();
        for (MerchantAccumulator acc : grouped.values()) {
            result.add(new MerchantWithConfigLine(
                    acc.mercId,
                    acc.name,
                    acc.hierarchyId,
                    acc.initiator,
                    acc.circuit,
                    acc.configuration
            ));
        }
        return result;
    }

    private static class MerchantAccumulator {
        private final Long mercId;
        private final String name;
        private final Long hierarchyId;
        private final String initiator;
        private final String circuit;
        private final LinkedHashMap<String, String> configuration = new LinkedHashMap<>();

        private MerchantAccumulator(Long mercId, String name, Long hierarchyId, String initiator, String circuit) {
            this.mercId = mercId;
            this.name = name;
            this.hierarchyId = hierarchyId;
            this.initiator = initiator;
            this.circuit = circuit;
        }
    }

    private String merchantSearchWhereClause(SearchTerm search) {
        if (!search.isPresent()) {
            return "";
        }
        return " WHERE (LOWER(m.NAME) LIKE :search OR LOWER(m.INITIATOR) LIKE :search OR LOWER(m.CIRCUIT) LIKE :search OR TO_CHAR(m.MERCID) LIKE :search)";
    }

    private String configSearchWhereClause(SearchTerm search) {
        if (!search.isPresent()) {
            return "";
        }
        return " AND (LOWER(PARAMETERNAME) LIKE :search OR LOWER(PARAMETERVALUE) LIKE :search)";
    }

    private String activeLineSearchWhereClause(SearchTerm search) {
        if (!search.isPresent()) {
            return "";
        }
        return " WHERE ("
                + "LOWER(m.NAME) LIKE :search "
                + "OR LOWER(m.INITIATOR) LIKE :search "
                + "OR LOWER(m.CIRCUIT) LIKE :search "
                + "OR TO_CHAR(m.MERCID) LIKE :search "
                + "OR EXISTS ("
                + "    SELECT 1 FROM MERC_CONFIG c2"
                + "    WHERE c2.MERCID = m.MERCID"
                + "      AND :atMoment >= c2.DATEBEGIN"
                + "      AND :atMoment < c2.DATEEND"
                + "      AND (LOWER(c2.PARAMETERNAME) LIKE :search OR LOWER(c2.PARAMETERVALUE) LIKE :search)"
                + ")"
                + ")";
    }

    private String merchantSortColumn(MerchantSortField sortBy) {
        return switch (sortBy) {
            case MERC_ID -> "m.MERCID";
            case NAME -> "m.NAME";
            case HIERARCHY_ID -> "m.HIERARCHYID";
            case INITIATOR -> "m.INITIATOR";
            case CIRCUIT -> "m.CIRCUIT";
        };
    }

    private String configSortColumn(MerchantConfigSortField sortBy) {
        return switch (sortBy) {
            case PARAMETER_NAME -> "PARAMETERNAME";
            case PARAMETER_VALUE -> "PARAMETERVALUE";
            case DATE_BEGIN -> "DATEBEGIN";
            case DATE_END -> "DATEEND";
        };
    }

    private void addSearchParam(MapSqlParameterSource params, SearchTerm search) {
        if (search.isPresent()) {
            params.addValue("search", search.likePattern());
        }
    }

    private Long readNullableLong(java.sql.ResultSet rs, String columnName) throws java.sql.SQLException {
        long value = rs.getLong(columnName);
        return rs.wasNull() ? null : value;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private OffsetDateTime toUtc(Timestamp timestamp) {
        return timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
