package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserInfoRepository {

    private static final String ADD_USER_INFO_QUERY = """
            insert into user_info(
                                  type_id,
                                  address_id,
                                  person_id
                                  )
            values (
                    :typeId,
                    :addressId,
                    :personId
                    )
            
            returning id
            """;

    private static final String ADD_ACHIEVER_PROFILE_QUERY = """
            insert into achiever_profile(
                                         user_id,
                                         level_id
                                         )
            values (
                    :userId,
                    :levelId
                    )
            """;

    private static final String FIND_PROFILE_BY_USER_ID_QUERY = """
            select ui.id,
                   ui.total_points,
                   ui.current_points,
                   ut.name,
                   a.postal_code
            from user_info ui
                     join user_type ut on ut.id = ui.type_id
                     join address a on a.id = ui.address_id
            where ui.id = :userId
            """;

    private static final String FIND_REWARD_STATE_FOR_UPDATE_QUERY = """
            select ui.id,
                   ui.total_points,
                   ui.current_points,
                   ui.habit_strength
            from user_info ui
            where ui.id = :userId
            for update
            """;

    private static final String FIND_TOTAL_POINTS_BY_USER_IDS_QUERY = """
            select id,
                   total_points
            from user_info
            where id in (:userIds)
            """;

    private static final String UPDATE_REWARD_STATE_QUERY = """
            update user_info
            set total_points = :totalPoints,
                current_points = :currentPoints,
                habit_strength = :habitStrength,
                updated_at = now()
            where id = :userId
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long addUserInfo(int typeId, long addressId, long personId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(ParameterNames.TYPE_ID, typeId)
                .addValue(ParameterNames.ADDRESS_ID, addressId)
                .addValue(ParameterNames.PERSON_ID, personId);

        Long id = namedParameterJdbcTemplate.queryForObject(ADD_USER_INFO_QUERY, params, Long.class);

        if (id == null) {
            throw new IllegalStateException("Не удалось получить id для user_info");
        }

        return id;
    }

    public void addAchieverProfile(long userId, int levelId) {
        namedParameterJdbcTemplate.update(
                ADD_ACHIEVER_PROFILE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.LEVEL_ID, levelId)
        );
    }

    public Optional<UserProfileInfo> findProfileByUserId(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_PROFILE_BY_USER_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> new UserProfileInfo(
                        rs.getLong(ColumnNames.ID),
                        UserType.fromDbName(rs.getString(ColumnNames.NAME)),
                        rs.getLong(ColumnNames.TOTAL_POINTS),
                        rs.getLong(ColumnNames.CURRENT_POINTS),
                        rs.getString(ColumnNames.POSTAL_CODE)
                )
        ).stream().findFirst();
    }

    /**
     * Читает баланс и силу привычки под блокировкой строки.
     * Используется для атомарного начисления/списания очков.
     */
    public Optional<UserRewardState> findRewardStateForUpdate(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_REWARD_STATE_FOR_UPDATE_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> new UserRewardState(
                        rs.getLong(ColumnNames.ID),
                        rs.getLong(ColumnNames.TOTAL_POINTS),
                        rs.getLong(ColumnNames.CURRENT_POINTS),
                        rs.getLong(ColumnNames.HABIT_STRENGTH)
                )
        ).stream().findFirst();
    }

    public void updateRewardState(long userId, long totalPoints, long currentPoints, long habitStrength) {
        namedParameterJdbcTemplate.update(
                UPDATE_REWARD_STATE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.TOTAL_POINTS, totalPoints)
                        .addValue(ParameterNames.CURRENT_POINTS, currentPoints)
                        .addValue(ParameterNames.HABIT_STRENGTH, habitStrength)
        );
    }

    public Map<Long, Long> findTotalPointsByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        List<Map.Entry<Long, Long>> rows = namedParameterJdbcTemplate.query(
                FIND_TOTAL_POINTS_BY_USER_IDS_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_IDS, userIds),
                (rs, rowNum) -> Map.entry(rs.getLong(ColumnNames.ID), rs.getLong(ColumnNames.TOTAL_POINTS))
        );

        Map<Long, Long> totalsByUserId = new HashMap<>();
        for (Map.Entry<Long, Long> row : rows) {
            totalsByUserId.put(row.getKey(), row.getValue());
        }
        return totalsByUserId;
    }
}
