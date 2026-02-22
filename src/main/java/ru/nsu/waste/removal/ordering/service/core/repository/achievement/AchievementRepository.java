package ru.nsu.waste.removal.ordering.service.core.repository.achievement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AchievementRepository {

    private static final String GET_BY_USER_ID_QUERY = """
            select id,
                   title,
                   description
            from achievement
            where user_type_id = :userTypeId
            order by id asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Achievement> findByUserType(int userTypeId) {
        return namedParameterJdbcTemplate.query(
                GET_BY_USER_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_TYPE_ID, userTypeId),
                (rs, rowNum) -> new Achievement(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION)
                )
        );
    }
}
