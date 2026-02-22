package ru.nsu.waste.removal.ordering.service.core.repository.ecotask;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTask;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class EcoTaskRepository {

    private static final String FIND_ACTIVE_BY_USER_TYPE_QUERY = """
            select id,
                   code,
                   title,
                   description,
                   points,
                   period
            from eco_task
            where user_type_id = :userTypeId
              and is_active = true
            order by id asc
            limit :limit
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<EcoTask> findActiveByUserType(int userTypeId, int limit) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue(ParameterNames.USER_TYPE_ID, userTypeId)
                .addValue(ParameterNames.LIMIT, limit);

        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_BY_USER_TYPE_QUERY,
                paramSource,
                (rs, rowNum) -> new EcoTask(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.CODE),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION),
                        rs.getLong(ColumnNames.POINTS),
                        EcoTaskPeriod.valueOf(rs.getString(ColumnNames.PERIOD))
                )
        );
    }
}
