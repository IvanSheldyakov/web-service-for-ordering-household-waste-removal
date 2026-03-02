package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.WasteFraction;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WasteFractionRepository {

    private static final String FIND_ACTIVE_FRACTIONS_QUERY = """
            select id,
                   type,
                   name,
                   is_active
            from waste_fraction
            where is_active = true
            order by name asc
            """;

    private static final String FIND_ACTIVE_FRACTIONS_BY_IDS_QUERY = """
            select id,
                   type,
                   name,
                   is_active
            from waste_fraction
            where id in (:fractionIds)
              and is_active = true
            order by name asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<WasteFraction> findActiveFractions() {
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_FRACTIONS_QUERY,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new WasteFraction(
                        rs.getLong(ColumnNames.ID),
                        rs.getString(ColumnNames.TYPE),
                        rs.getString(ColumnNames.NAME),
                        rs.getBoolean(ColumnNames.IS_ACTIVE)
                )
        );
    }

    public List<WasteFraction> findActiveFractionsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_FRACTIONS_BY_IDS_QUERY,
                new MapSqlParameterSource(ParameterNames.FRACTION_IDS, ids),
                (rs, rowNum) -> new WasteFraction(
                        rs.getLong(ColumnNames.ID),
                        rs.getString(ColumnNames.TYPE),
                        rs.getString(ColumnNames.NAME),
                        rs.getBoolean(ColumnNames.IS_ACTIVE)
                )
        );
    }
}
