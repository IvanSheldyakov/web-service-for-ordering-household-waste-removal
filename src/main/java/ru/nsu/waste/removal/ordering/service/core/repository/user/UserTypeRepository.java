package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserTypeRepository {

    private static final String FIND_ALL_QUERY = """
            select id,
                   name
            from user_type
            order by id asc
            """;

    private static final String FIND_BY_NAME_QUERY = """
            select id,
                   name
            from user_type
            where name = :name
            """;

    private static final String FIND_BY_ID_QUERY = """
            select id,
                   name
            from user_type
            where id = :id
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<UserType> findAll() {
        return namedParameterJdbcTemplate.query(
                FIND_ALL_QUERY,
                Map.of(),
                (rs, rowNum) -> new UserType(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.NAME)
                )
        );
    }

    public Optional<UserType> findByName(String name) {
        return namedParameterJdbcTemplate.query(
                FIND_BY_NAME_QUERY,
                new MapSqlParameterSource(ParameterNames.NAME, name),
                (rs, rowNum) -> new UserType(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.NAME)
                )
        ).stream().findFirst();
    }

    public Optional<UserType> findById(int id) {
        return namedParameterJdbcTemplate.query(
                FIND_BY_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.ID, id),
                (rs, rowNum) -> new UserType(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.NAME)
                )
        ).stream().findFirst();
    }
}
