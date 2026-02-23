package ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuiz;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RegistrationQuizRepository {

    private static final String FIND_LATEST_ACTIVE_BY_CODE_QUERY = """
            select id,
                   code,
                   version
            from registration_quiz
            where is_active = true
              and code = :code
            order by version desc
            limit 1
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<RegistrationQuiz> findLatestActiveByCode(String code) {
        return namedParameterJdbcTemplate.query(
                FIND_LATEST_ACTIVE_BY_CODE_QUERY,
                new MapSqlParameterSource(ParameterNames.CODE, code),
                (rs, rowNum) -> new RegistrationQuiz(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.CODE),
                        rs.getInt(ColumnNames.VERSION)
                )
        ).stream().findFirst();
    }
}
