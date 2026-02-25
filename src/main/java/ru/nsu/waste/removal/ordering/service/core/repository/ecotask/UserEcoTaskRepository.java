package ru.nsu.waste.removal.ordering.service.core.repository.ecotask;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserEcoTaskRepository {

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
                                      expired_at
                                      )
            values (
                    :userId,
                    :ecoTaskId,
                    'ASSIGNED',
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

    public void addAssigned(long userId, int ecoTaskId, OffsetDateTime expiredAt) {
        namedParameterJdbcTemplate.update(
                INSERT_ASSIGNED_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.ECO_TASK_ID, ecoTaskId)
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
}
