package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserLeaderboardRepository {

    private static final String FIND_WEEKLY_RANK_POSITION_QUERY = """
            with target_user as (
                select a.postal_code
                from user_info ui
                         join address a on a.id = ui.address_id
                where ui.id = :userId
            ),
                 totals as (
                     select ui.id as user_id,
                            coalesce(sum(uah.points_difference), 0) as score
                     from user_info ui
                              join address a on a.id = ui.address_id
                              join target_user tu on tu.postal_code = a.postal_code
                              left join user_action_history uah
                                        on uah.user_id = ui.id
                                            and uah.created_at >= :since
                     group by ui.id
                 ),
                 ranked as (
                     select user_id,
                            dense_rank() over (order by score desc) as rank_position
                     from totals
                 )
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
}
