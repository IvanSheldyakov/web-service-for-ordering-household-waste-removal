package ru.nsu.waste.removal.ordering.service.core.repository.history;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

@Repository
@RequiredArgsConstructor
public class UserActionHistoryRepository {

    private static final String ADD_EVENT_QUERY = """
            insert into user_action_history(
                                          user_id,
                                          event_type,
                                          content,
                                          points_difference
                                          )
            values (
                    :userId,
                    :eventType,
                    cast(:content as jsonb),
                    :pointsDifference
                    )
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void addEvent(long userId, String eventType, String contentJson, long pointsDifference) {
        namedParameterJdbcTemplate.update(
                ADD_EVENT_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.EVENT_TYPE, eventType)
                        .addValue(ParameterNames.CONTENT, contentJson)
                        .addValue(ParameterNames.POINTS_DIFFERENCE, pointsDifference)
        );
    }
}