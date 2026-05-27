package ru.copperside.core.infrastructure;

import ru.copperside.core.domain.Terminal;
import ru.copperside.core.domain.TerminalPage;
import ru.copperside.core.domain.TerminalRepository;
import ru.copperside.core.domain.TerminalSortField;
import ru.copperside.core.domain.PageWindow;
import ru.copperside.core.domain.SearchTerm;
import ru.copperside.core.domain.SortOrder;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;

@Repository
public class OracleTerminalRepository implements TerminalRepository {

    private static final String BASE_SELECT = """
            SELECT t.MERCID, t.MPS, t.GATE, t.IS3DS, t.TERMINALID, t.MERCHANTID,
                   t.MCC, t.NAME, t.MERCHANTURL, t.LOGIN,
                   CASE WHEN t.PASSWORD IS NOT NULL AND LENGTH(t.PASSWORD) > 0 THEN 1 ELSE 0 END AS HAS_PASSWORD,
                   t.APIURL,
                   m.NAME AS MERCHANT_NAME,
                   COUNT(*) OVER () AS TOTAL_COUNT
            FROM TERMINALSETTINGS t
            LEFT JOIN "AP#MERCHANTS" m ON m.MERCID = t.MERCID
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OracleTerminalRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public TerminalPage findAll(PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort) {
        return query(null, page, search, sort);
    }

    @Override
    public TerminalPage findByMercId(Long mercId, PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort) {
        return query(mercId, page, search, sort);
    }

    private TerminalPage query(Long mercId, PageWindow page, SearchTerm search, SortOrder<TerminalSortField> sort) {
        String sql = BASE_SELECT
                + " WHERE (:mercId IS NULL OR t.MERCID = :mercId)"
                + "   AND (:search IS NULL OR ("
                + "        LOWER(TO_CHAR(t.MERCID)) LIKE :search"
                + "     OR LOWER(t.MPS) LIKE :search"
                + "     OR LOWER(t.GATE) LIKE :search"
                + "     OR LOWER(t.TERMINALID) LIKE :search"
                + "     OR LOWER(t.MERCHANTID) LIKE :search"
                + "     OR LOWER(t.MCC) LIKE :search"
                + "     OR LOWER(t.NAME) LIKE :search"
                + "     OR LOWER(m.NAME) LIKE :search"
                + "   ))"
                + " ORDER BY " + sortColumn(sort.field()) + " " + sort.direction().name() + " NULLS LAST,"
                + "          t.MERCID, t.MPS, t.GATE, t.TERMINALID"
                + " OFFSET :offset ROWS FETCH NEXT :limit ROWS ONLY";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("mercId", mercId, Types.BIGINT)
                .addValue("search", search.likePattern())
                .addValue("limit", page.limit())
                .addValue("offset", page.offset());

        long[] total = {0L};
        List<Terminal> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            total[0] = rs.getLong("TOTAL_COUNT");
            return new Terminal(
                    rs.getLong("MERCID"),
                    rs.getString("MPS"),
                    rs.getString("GATE"),
                    rs.getInt("IS3DS") == 1,
                    rs.getString("TERMINALID"),
                    rs.getString("MERCHANTID"),
                    rs.getString("MCC"),
                    rs.getString("NAME"),
                    rs.getString("MERCHANTURL"),
                    rs.getString("LOGIN"),
                    rs.getInt("HAS_PASSWORD") == 1,
                    rs.getString("APIURL"),
                    rs.getString("MERCHANT_NAME")
            );
        });

        return new TerminalPage(rows, rows.isEmpty() ? 0L : total[0]);
    }

    private String sortColumn(TerminalSortField field) {
        return switch (field) {
            case MERC_ID -> "t.MERCID";
            case MPS -> "t.MPS";
            case GATE -> "t.GATE";
            case TERMINAL_ID -> "t.TERMINALID";
            case MCC -> "t.MCC";
        };
    }
}
