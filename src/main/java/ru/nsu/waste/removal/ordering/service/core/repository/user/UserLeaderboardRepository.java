package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserLeaderboardEntry;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserLeaderboardRepository {

    private static final String LEADERBOARD_RANKING_CTE = """
            with target_user as (
                select a.postal_code
                from user_info ui
                         join address a on a.id = ui.address_id
                where ui.id = :userId
            ),
                 totals as (
                     select ui.id as user_id,
                            trim(concat_ws(' ', p.name, p.surname)) as full_name,
                            coalesce(sum(
                                case
                                    when uah.points_difference > 0 then uah.points_difference
                                    else 0
                                end
                            ), 0) as score
                     from user_info ui
                              join address a on a.id = ui.address_id
                              join person_info p on p.id = ui.person_id
                              join target_user tu on tu.postal_code = a.postal_code
                              left join user_action_history uah
                                        on uah.user_id = ui.id
                                            and uah.created_at >= :since
                     group by ui.id, p.name, p.surname
                 ),
                 ranked as (
                     select user_id,
                            full_name,
                            score,
                            dense_rank() over (order by score desc) as rank_position
                     from totals
                 )
            """;

    private static final String FIND_RANK_ENTRY_QUERY = LEADERBOARD_RANKING_CTE + """
            select user_id,
                   full_name,
                   score,
                   rank_position
            from ranked
            where user_id = :userId
            """;

    private static final String FIND_TOP_ENTRIES_QUERY = LEADERBOARD_RANKING_CTE + """
            select user_id,
                   full_name,
                   score,
                   rank_position
            from ranked
            order by rank_position asc, user_id asc
            limit :limit
            """;

    private static final String FIND_WEEKLY_RANK_POSITION_QUERY = LEADERBOARD_RANKING_CTE + """
            select rank_position
            from ranked
            where user_id = :userId
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<Integer> findWeeklyRankPosition(long userId, OffsetDateTime since) {
        return namedParameterJdbcTemplate.query(
                FIND_WEEKLY_RANK_POSITION_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.SINCE, since),
                (rs, rowNum) -> rs.getInt(ColumnNames.RANK_POSITION)
        ).stream().findFirst();
    }

    public Optional<UserLeaderboardEntry> findRankEntry(long userId, OffsetDateTime since) {
        return namedParameterJdbcTemplate.query(
                FIND_RANK_ENTRY_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.SINCE, since),
                (rs, rowNum) -> new UserLeaderboardEntry(
                        rs.getLong(ColumnNames.USER_ID),
                        rs.getString(ColumnNames.FULL_NAME),
                        rs.getInt(ColumnNames.RANK_POSITION),
                        rs.getLong(ColumnNames.SCORE)
                )
        ).stream().findFirst();
    }

    public List<UserLeaderboardEntry> findTopEntriesByUserDistrict(long userId, OffsetDateTime since, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return namedParameterJdbcTemplate.query(
                FIND_TOP_ENTRIES_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.SINCE, since)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> new UserLeaderboardEntry(
                        rs.getLong(ColumnNames.USER_ID),
                        rs.getString(ColumnNames.FULL_NAME),
                        rs.getInt(ColumnNames.RANK_POSITION),
                        rs.getLong(ColumnNames.SCORE)
                )
        );
    }
}
