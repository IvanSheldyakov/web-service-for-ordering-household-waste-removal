package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.level.AchieverLevelTarget;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AchieverProfileRepository {

    private static final String FIND_LEVEL_TARGET_FOR_UPDATE_QUERY = """
            select ap.user_id,
                   ap.level_id,
                   l.required_total_points
            from achiever_profile ap
                     join level l on l.id = ap.level_id
            where ap.user_id = :userId
            for update
            """;

    private static final String UPDATE_LEVEL_QUERY = """
            update achiever_profile
            set level_id = :levelId
            where user_id = :userId
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<AchieverLevelTarget> findLevelTargetForUpdate(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_LEVEL_TARGET_FOR_UPDATE_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> new AchieverLevelTarget(
                        rs.getLong(ColumnNames.USER_ID),
                        rs.getInt(ColumnNames.LEVEL_ID),
                        rs.getInt(ColumnNames.REQUIRED_TOTAL_POINTS)
                )
        ).stream().findFirst();
    }

    public void updateLevel(long userId, int levelId) {
        namedParameterJdbcTemplate.update(
                UPDATE_LEVEL_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.LEVEL_ID, levelId)
        );
    }
}
