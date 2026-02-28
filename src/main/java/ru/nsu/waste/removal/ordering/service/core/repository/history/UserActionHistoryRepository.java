package ru.nsu.waste.removal.ordering.service.core.repository.history;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

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

    private static final String FIND_EVENTS_AFTER_ID_QUERY = """
            select id,
                   user_id,
                   event_type,
                   points_difference
            from user_action_history
            where id > :id
            order by id asc
            limit :limit
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

    public List<UserActionHistoryEvent> findEventsAfterId(long id, int limit) {
        return namedParameterJdbcTemplate.query(
                FIND_EVENTS_AFTER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.ID, id)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> new UserActionHistoryEvent(
                        rs.getLong(ColumnNames.ID),
                        rs.getLong(ColumnNames.USER_ID),
                        UserActionEventType.fromDbName(rs.getString(ColumnNames.EVENT_TYPE)),
                        rs.getLong(ColumnNames.POINTS_DIFFERENCE)
                )
        );
    }

}
