package ru.nsu.waste.removal.ordering.service.core.repository.level;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Optional;

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

    private static final String FIND_HIGHEST_LEVEL_QUERY = """
            select id,
                   required_total_points
            from level
            order by required_total_points desc
            limit 1
            """;

    /**
     * Возвращает "следующую цель" по уровню: минимальный уровень, порог которого строго больше текущих totalPoints.
     * <p>
     * Пример:
     * - totalPoints = 0 -> 1000
     * - totalPoints = 999 -> 1000
     * - totalPoints = 1000 -> 2000
     * - totalPoints = 3200 -> 4000
     * - totalPoints >= max -> Optional.empty()
     */
    private static final String FIND_NEXT_TARGET_LEVEL_BY_TOTAL_POINTS_QUERY = """
            select id,
                   required_total_points
            from level
            where required_total_points > :totalPoints
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

    public Level findHighestLevel() {
        return namedParameterJdbcTemplate.queryForObject(
                FIND_HIGHEST_LEVEL_QUERY,
                new MapSqlParameterSource(),
                (rs, rowNum) -> new Level(
                        rs.getInt(ColumnNames.ID),
                        rs.getInt(ColumnNames.REQUIRED_TOTAL_POINTS)
                )
        );
    }

    public Optional<Level> findNextTargetLevelByTotalPoints(long totalPoints) {
        return namedParameterJdbcTemplate.query(
                FIND_NEXT_TARGET_LEVEL_BY_TOTAL_POINTS_QUERY,
                new MapSqlParameterSource(ParameterNames.TOTAL_POINTS, totalPoints),
                (rs, rowNum) -> new Level(
                        rs.getInt(ColumnNames.ID),
                        rs.getInt(ColumnNames.REQUIRED_TOTAL_POINTS)
                )
        ).stream().findFirst();
    }
}