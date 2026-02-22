package ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegistrationQuizQuestionRepository {

    private static final String FIND_ACTIVE_BY_QUIZ_ID_QUERY = """
            select id,
                   quiz_id,
                   code,
                   ord,
                   text,
                   is_tiebreak
            from registration_quiz_question
            where quiz_id = :quizId
              and is_active = true
            order by ord asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<RegistrationQuizQuestion> findActiveByQuizId(int quizId) {
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_BY_QUIZ_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.QUIZ_ID, quizId),
                (rs, rowNum) -> new RegistrationQuizQuestion(
                        rs.getInt(ColumnNames.ID),
                        rs.getInt(ColumnNames.QUIZ_ID),
                        rs.getString(ColumnNames.CODE),
                        rs.getInt(ColumnNames.ORD),
                        rs.getString(ColumnNames.TEXT),
                        rs.getBoolean(ColumnNames.IS_TIEBREAK)
                )
        );
    }
}
