package ru.nsu.waste.removal.ordering.service.core.repository.achievement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AchievementUserRepository {

    private static final String UNLOCK_ACHIEVEMENT_FOR_USER_QUERY = """
            insert into achievement_user(
                                         achievement_id,
                                         user_id
                                         )
            values (
                    :achievementId,
                    :userId
                    )
            on conflict do nothing
            """;

    private static final String FIND_UNLOCKED_ACHIEVEMENT_IDS_BY_USER_ID_QUERY = """
            select achievement_id
            from achievement_user
            where user_id = :userId
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean unlockForUser(long userId, int achievementId) {
        int updatedRows = namedParameterJdbcTemplate.update(
                UNLOCK_ACHIEVEMENT_FOR_USER_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.ACHIEVEMENT_ID, achievementId)
                        .addValue(ParameterNames.USER_ID, userId)
        );
        return updatedRows > 0;
    }

    public Set<Integer> findUnlockedAchievementIdsByUserId(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_UNLOCKED_ACHIEVEMENT_IDS_BY_USER_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> rs.getInt(ColumnNames.ACHIEVEMENT_ID)
        ).stream().collect(Collectors.toSet());
    }
}
