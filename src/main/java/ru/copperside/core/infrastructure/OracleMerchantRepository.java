package ru.copperside.core.infrastructure;

import ru.copperside.core.domain.Merchant;
import ru.copperside.core.domain.MerchantConfigEntry;
import ru.copperside.core.domain.MerchantConfigSortField;
import ru.copperside.core.domain.MerchantRepository;
import ru.copperside.core.domain.MerchantSortField;
import ru.copperside.core.domain.MerchantWithConfigLine;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.MerchantAdminLine;
import ru.copperside.core.domain.MerchantAdminPage;
import ru.copperside.core.domain.MerchantAdminSortField;
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
import java.util.Locale;
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

    private static final String ADMIN_PROJECTION_CTE = """
            WITH active_cfg AS (
                SELECT c.MERCID,
                       MIN(c.DATEBEGIN) AS CREATED_AT,
                       COUNT(*) AS CFG_COUNT,
                       MAX(CASE WHEN LOWER(c.PARAMETERNAME) = 'status'
                                THEN LOWER(c.PARAMETERVALUE) END) AS EXPLICIT_STATUS,
                       MAX(CASE WHEN LOWER(c.PARAMETERNAME) IN
                                     ('ecomallowed','directecomallow','onlineecomenabled',
                                      'onlinep2penabled','p2pdebitallowed','p2pcreditallowed')
                                 AND LOWER(TRIM(c.PARAMETERVALUE)) IN ('0','false','no','n','disabled')
                                THEN 1 ELSE 0 END) AS ANY_DISABLED
                FROM MERC_CONFIG c
                WHERE :atMoment >= c.DATEBEGIN AND :atMoment < c.DATEEND
                GROUP BY c.MERCID
            ),
            mcc_pick AS (
                SELECT MERCID, MCC FROM (
                    SELECT c.MERCID, c.PARAMETERVALUE AS MCC,
                           ROW_NUMBER() OVER (PARTITION BY c.MERCID ORDER BY c.PARAMETERNAME) AS RN
                    FROM MERC_CONFIG c
                    WHERE :atMoment >= c.DATEBEGIN AND :atMoment < c.DATEEND
                      AND LOWER(c.PARAMETERNAME) IN ('mcc','merchant_mcc','merchantcategorycode')
                      AND LENGTH(c.PARAMETERVALUE) = 4
                      AND TRANSLATE(c.PARAMETERVALUE, '0123456789', '0000000000') = '0000'
                )
                WHERE RN = 1
            ),
            proj AS (
                SELECT m.MERCID,
                       m.NAME,
                       CASE
                           WHEN ac.MERCID IS NULL OR ac.CFG_COUNT = 0 THEN 'blocked'
                           WHEN ac.EXPLICIT_STATUS = 'blocked' THEN 'blocked'
                           WHEN ac.EXPLICIT_STATUS = 'suspended' THEN 'suspended'
                           WHEN ac.ANY_DISABLED = 1 THEN 'suspended'
                           ELSE 'active'
                       END AS STATUS,
                       COALESCE(mp.MCC, '0000') AS MCC,
                       ac.CREATED_AT
                FROM "AP#MERCHANTS" m
                LEFT JOIN active_cfg ac ON ac.MERCID = m.MERCID
                LEFT JOIN mcc_pick mp ON mp.MERCID = m.MERCID
            )
            SELECT p.MERCID, p.NAME, p.STATUS, p.MCC, p.CREATED_AT, COUNT(*) OVER () AS TOTAL_COUNT
            FROM proj p
            WHERE (:status IS NULL OR p.STATUS = :status)
              AND (:search IS NULL OR (
                    LOWER('MRC-' || LPAD(TO_CHAR(p.MERCID), 5, '0')) LIKE :search
                    OR LOWER(p.NAME) LIKE :search
                    OR p.STATUS LIKE :search
                    OR p.MCC LIKE :search
              ))
            """;

    private static final String ADMIN_PROJECTION_TAIL =
            " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";

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
                c.PARAMETERVALUE,
                c.DATEBEGIN
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
    public long countWithActiveConfigLine(SearchTerm search) {
        String sql = "SELECT COUNT(*) FROM \"AP#MERCHANTS\" m" + merchantSearchWhereClause(search);
        MapSqlParameterSource params = new MapSqlParameterSource();
        addSearchParam(params, search);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count == null ? 0L : count;
    }

    @Override
    public MerchantAdminPage findAdminProjection(
            OffsetDateTime atMoment,
            PageWindow page,
            SearchTerm search,
            String status,
            SortOrder<MerchantAdminSortField> sort
    ) {
        String sql = ADMIN_PROJECTION_CTE
                + " ORDER BY " + adminSortColumn(sort.field()) + " " + sort.direction().name()
                + ADMIN_PROJECTION_TAIL;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("atMoment", Timestamp.from(atMoment.toInstant()))
                .addValue("limit", page.limit())
                .addValue("offset", page.offset())
                .addValue("status", (status == null || status.isBlank())
                        ? null : status.trim().toLowerCase(Locale.ROOT))
                .addValue("search", search.likePattern());

        long[] total = {0L};
        List<MerchantAdminLine> lines = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            total[0] = rs.getLong("TOTAL_COUNT");
            Timestamp createdAt = rs.getTimestamp("CREATED_AT");
            return new MerchantAdminLine(
                    rs.getLong("MERCID"),
                    rs.getString("NAME"),
                    rs.getString("STATUS"),
                    rs.getString("MCC"),
                    createdAt == null ? null : toUtc(createdAt)
            );
        });

        return new MerchantAdminPage(lines, lines.isEmpty() ? 0L : total[0]);
    }

    private String adminSortColumn(MerchantAdminSortField sortBy) {
        return switch (sortBy) {
            case MERC_ID -> "p.MERCID";
            case NAME -> "p.NAME";
            case STATUS -> "p.STATUS";
            case MCC -> "p.MCC";
            case CREATED_AT -> "p.CREATED_AT";
        };
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

            Timestamp dateBegin = (Timestamp) row.get("DATEBEGIN");
            if (dateBegin != null) {
                OffsetDateTime begin = toUtc(dateBegin);
                if (acc.activeSince == null || begin.isBefore(acc.activeSince)) {
                    acc.activeSince = begin;
                }
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
                    acc.configuration,
                    acc.activeSince
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
        private OffsetDateTime activeSince;

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
