package ru.nsu.waste.removal.ordering.service.core.repository.achievement;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AchievementRepository {

    private static final String FIND_BY_USER_TYPE_ID_QUERY = """
            select id,
                   title,
                   description
            from achievement
            where user_type_id = :userTypeId
            order by id asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<Achievement> findByUserType(UserType userType) {
        return namedParameterJdbcTemplate.query(
                FIND_BY_USER_TYPE_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_TYPE_ID, userType.getId()),
                (rs, rowNum) -> new Achievement(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION)
                )
        );
    }
}
