package ru.nsu.waste.removal.ordering.service.core.repository.ecotask;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.ActiveEcoTaskAssignment;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskRuleType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserEcoTaskRepository {

    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_DONE = "DONE";

    private static final String EXISTS_ACTIVE_ASSIGNMENT_QUERY = """
            select exists(
                select 1
                from user_eco_task
                where user_id = :userId
                  and eco_task_id = :ecoTaskId
                  and status = 'ASSIGNED'
            )
            """;

    private static final String INSERT_ASSIGNED_QUERY = """
            insert into user_eco_task(
                                      user_id,
                                      eco_task_id,
                                      status,
                                      assigned_at,
                                      expired_at
                                      )
            values (
                    :userId,
                    :ecoTaskId,
                    'ASSIGNED',
                    :assignedAt,
                    :expiredAt
                    )
            """;

    private static final String FIND_ASSIGNED_BY_USER_ID_QUERY = """
            select uet.id,
                   uet.eco_task_id,
                   et.title,
                   et.description,
                   et.points,
                   uet.expired_at
            from user_eco_task uet
                     join eco_task et on et.id = uet.eco_task_id
            where uet.user_id = :userId
              and uet.status = 'ASSIGNED'
            order by uet.id asc
            """;

    private static final String FIND_ACTIVE_ASSIGNMENTS_BY_TRIGGER_EVENT_QUERY = """
            select uet.id as user_eco_task_id,
                   et.id as eco_task_id,
                   et.code as code,
                   et.points as points,
                   (et.rule ->> 'type') as rule_type,
                   cast(et.rule ->> 'target' as bigint) as target,
                   (et.rule -> 'filters' ->> 'type') as order_type_filter,
                   (et.rule -> 'filters' ->> 'status') as order_status_filter,
                   cast(et.rule -> 'filters' ->> 'green_chosen' as boolean) as green_chosen_filter,
                   (et.rule -> 'filters' ->> 'event_type') as action_event_type_filter,
                   uet.assigned_at as assigned_at,
                   uet.expired_at as expired_at
            from user_eco_task uet
                     join eco_task et on et.id = uet.eco_task_id
            where uet.user_id = :userId
              and uet.status = :status
              and et.trigger_event = :triggerEvent
              and uet.expired_at >= :now
            order by uet.id asc
            """;

    private static final String MARK_DONE_QUERY = """
            update user_eco_task
            set status = :status,
                completed_at = now()
            where id = :userEcoTaskId
              and status = :assignedStatus
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean existsActiveAssignment(long userId, int ecoTaskId) {
        Boolean exists = namedParameterJdbcTemplate.queryForObject(
                EXISTS_ACTIVE_ASSIGNMENT_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.ECO_TASK_ID, ecoTaskId),
                Boolean.class
        );
        return exists != null && exists;
    }

    public void addAssigned(long userId, int ecoTaskId, OffsetDateTime assignedAt, OffsetDateTime expiredAt) {
        namedParameterJdbcTemplate.update(
                INSERT_ASSIGNED_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.ECO_TASK_ID, ecoTaskId)
                        .addValue(ParameterNames.ASSIGNED_AT, assignedAt)
                        .addValue(ParameterNames.EXPIRED_AT, expiredAt)
        );
    }

    public List<AssignedEcoTask> findAssignedByUserId(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_ASSIGNED_BY_USER_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> new AssignedEcoTask(
                        rs.getLong(ColumnNames.ID),
                        rs.getLong(ColumnNames.ECO_TASK_ID),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION),
                        rs.getLong(ColumnNames.POINTS),
                        rs.getObject(ColumnNames.EXPIRED_AT, OffsetDateTime.class)
                )
        );
    }

    public List<ActiveEcoTaskAssignment> findActiveAssignmentsByUserIdAndTriggerEvent(
            long userId,
            UserActionEventType triggerEvent,
            OffsetDateTime now
    ) {
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_ASSIGNMENTS_BY_TRIGGER_EVENT_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.STATUS, STATUS_ASSIGNED)
                        .addValue(ParameterNames.TRIGGER_EVENT, triggerEvent.dbName())
                        .addValue(ParameterNames.NOW, now),
                (rs, rowNum) -> {
                    String actionEventTypeFilterValue = rs.getString(ColumnNames.ACTION_EVENT_TYPE_FILTER);
                    UserActionEventType actionEventTypeFilter = actionEventTypeFilterValue == null
                            ? null
                            : UserActionEventType.fromDbName(actionEventTypeFilterValue);

                    return new ActiveEcoTaskAssignment(
                            rs.getLong(ColumnNames.USER_ECO_TASK_ID),
                            rs.getInt(ColumnNames.ECO_TASK_ID),
                            rs.getString(ColumnNames.CODE),
                            rs.getLong(ColumnNames.POINTS),
                            EcoTaskRuleType.valueOf(rs.getString(ColumnNames.RULE_TYPE)),
                            rs.getLong(ColumnNames.TARGET),
                            rs.getString(ColumnNames.ORDER_TYPE_FILTER),
                            rs.getString(ColumnNames.ORDER_STATUS_FILTER),
                            (Boolean) rs.getObject(ColumnNames.GREEN_CHOSEN_FILTER),
                            actionEventTypeFilter,
                            rs.getObject(ColumnNames.ASSIGNED_AT, OffsetDateTime.class),
                            rs.getObject(ColumnNames.EXPIRED_AT, OffsetDateTime.class)
                    );
                }
        );
    }

    public boolean markDone(long userEcoTaskId) {
        int updatedRows = namedParameterJdbcTemplate.update(
                MARK_DONE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ECO_TASK_ID, userEcoTaskId)
                        .addValue(ParameterNames.STATUS, STATUS_DONE)
                        .addValue(ParameterNames.ASSIGNED_STATUS, STATUS_ASSIGNED)
        );
        return updatedRows > 0;
    }
}
