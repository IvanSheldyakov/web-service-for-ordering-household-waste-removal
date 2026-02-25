package ru.nsu.waste.removal.ordering.service.core.repository.level;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;

@Repository
@RequiredArgsConstructor
public class LevelRepository {

    private static final String FIND_LOWEST_LEVEL_QUERY = """
            select id,
                   required_total_points
            from level
            order by required_total_points asc
            limit 1
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Level findLowestLevel() {
        return namedParameterJdbcTemplate.queryForObject(
                FIND_LOWEST_LEVEL_QUERY,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new Level(
                        rs.getInt(ColumnNames.ID),
                        rs.getInt(ColumnNames.REQUIRED_TOTAL_POINTS)
                )
        );
    }
}
