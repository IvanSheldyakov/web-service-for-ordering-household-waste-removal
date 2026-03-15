package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class SortingRegularityWindowRepository {

    private static final String ADD_WINDOW_IF_ABSENT_QUERY = """
            insert into sorting_regularity_window(
                user_id,
                window_start,
                window_end,
                status
            )
            values (
                :userId,
                :windowStart,
                :windowEnd,
                :status
            )
            on conflict do nothing
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean addWindowIfAbsent(
            long userId,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            String status
    ) {
        int updatedRows = namedParameterJdbcTemplate.update(
                ADD_WINDOW_IF_ABSENT_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.WINDOW_START, windowStart)
                        .addValue(ParameterNames.WINDOW_END, windowEnd)
                        .addValue(ParameterNames.STATUS, status)
        );
        return updatedRows > 0;
    }
}
