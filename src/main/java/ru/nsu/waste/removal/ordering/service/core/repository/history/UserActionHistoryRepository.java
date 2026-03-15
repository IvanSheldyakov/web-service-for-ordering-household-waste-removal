package ru.nsu.waste.removal.ordering.service.core.repository.history;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryRecord;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.AddEventParams;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.CountByUserIdAndEventTypeInPeriodParams;

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
                   created_at,
                   user_id,
                   event_type,
                   points_difference,
                   content
            from user_action_history
            where id > :id
            order by id asc
            limit :limit
            """;

    private static final String COUNT_EVENTS_BY_USER_ID_AND_TYPE_QUERY = """
            select count(*)
            from user_action_history
            where user_id = :userId
              and event_type = :eventType
            """;

    private static final String COUNT_EVENTS_BY_USER_ID_AND_TYPE_IN_PERIOD_QUERY = """
            select count(*)
            from user_action_history
            where user_id = :userId
              and event_type = :eventType
              and created_at >= :from
              and created_at <= :to
            """;

    private static final String FIND_POINTS_DIFFERENCE_BY_EVENT_ID_QUERY = """
            select points_difference
            from user_action_history
            where id = :id
            """;

    private static final String UPDATE_EVENT_REWARD_BY_ID_QUERY = """
            update user_action_history
            set content = cast(:content as jsonb),
                points_difference = :pointsDifference
            where id = :id
            """;

    private static final String FIND_LATEST_EVENTS_BY_USER_ID_QUERY = """
            select id,
                   user_id,
                   created_at,
                   event_type,
                   points_difference,
                   content
            from user_action_history
            where user_id = :userId
              and event_type in (:eventTypes)
            order by created_at desc,
                     id desc
            limit :limit
            """;

    private static final String FIND_LATEST_NEGATIVE_EVENTS_BY_USER_ID_QUERY = """
            select id,
                   user_id,
                   created_at,
                   event_type,
                   points_difference,
                   content
            from user_action_history
            where user_id = :userId
              and points_difference < 0
            order by created_at desc,
                     id desc
            limit :limit
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public void addEvent(AddEventParams params) {
        namedParameterJdbcTemplate.update(
                ADD_EVENT_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, params.userId())
                        .addValue(ParameterNames.EVENT_TYPE, params.eventType())
                        .addValue(ParameterNames.CONTENT, params.contentJson())
                        .addValue(ParameterNames.POINTS_DIFFERENCE, params.pointsDifference())
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
                        rs.getObject(ColumnNames.CREATED_AT, java.time.OffsetDateTime.class),
                        rs.getLong(ColumnNames.USER_ID),
                        UserActionEventType.fromDbName(rs.getString(ColumnNames.EVENT_TYPE)),
                        rs.getLong(ColumnNames.POINTS_DIFFERENCE),
                        rs.getString(ColumnNames.CONTENT)
                )
        );
    }

    public long countByUserIdAndEventType(long userId, String eventType) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_EVENTS_BY_USER_ID_AND_TYPE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.EVENT_TYPE, eventType),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countByUserIdAndEventTypeInPeriod(CountByUserIdAndEventTypeInPeriodParams params) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_EVENTS_BY_USER_ID_AND_TYPE_IN_PERIOD_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, params.userId())
                        .addValue(ParameterNames.EVENT_TYPE, params.eventType())
                        .addValue(ParameterNames.FROM, params.from())
                        .addValue(ParameterNames.TO, params.to()),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long findPointsDifferenceByEventId(long eventId) {
        Long pointsDifference = namedParameterJdbcTemplate.queryForObject(
                FIND_POINTS_DIFFERENCE_BY_EVENT_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.ID, eventId),
                Long.class
        );
        return pointsDifference == null ? 0L : pointsDifference;
    }

    public void updateEventRewardById(long eventId, String contentJson, long pointsDifference) {
        namedParameterJdbcTemplate.update(
                UPDATE_EVENT_REWARD_BY_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.ID, eventId)
                        .addValue(ParameterNames.CONTENT, contentJson)
                        .addValue(ParameterNames.POINTS_DIFFERENCE, pointsDifference)
        );
    }

    public List<UserActionHistoryRecord> findLatestEventsByUserId(long userId, List<String> eventTypes, int limit) {
        if (eventTypes == null || eventTypes.isEmpty() || limit <= 0) {
            return List.of();
        }
        return namedParameterJdbcTemplate.query(
                FIND_LATEST_EVENTS_BY_USER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.EVENT_TYPES, eventTypes)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> new UserActionHistoryRecord(
                        rs.getLong(ColumnNames.ID),
                        rs.getLong(ColumnNames.USER_ID),
                        rs.getObject(ColumnNames.CREATED_AT, java.time.OffsetDateTime.class),
                        rs.getString(ColumnNames.EVENT_TYPE),
                        rs.getLong(ColumnNames.POINTS_DIFFERENCE),
                        rs.getString(ColumnNames.CONTENT)
                )
        );
    }

    public List<UserActionHistoryRecord> findLatestNegativeEventsByUserId(long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }
        return namedParameterJdbcTemplate.query(
                FIND_LATEST_NEGATIVE_EVENTS_BY_USER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> new UserActionHistoryRecord(
                        rs.getLong(ColumnNames.ID),
                        rs.getLong(ColumnNames.USER_ID),
                        rs.getObject(ColumnNames.CREATED_AT, java.time.OffsetDateTime.class),
                        rs.getString(ColumnNames.EVENT_TYPE),
                        rs.getLong(ColumnNames.POINTS_DIFFERENCE),
                        rs.getString(ColumnNames.CONTENT)
                )
        );
    }
}
