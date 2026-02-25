package ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegistrationQuizOptionRepository {

    private static final String FIND_ACTIVE_BY_QUESTION_IDS_QUERY = """
            select id,
                   question_id,
                   ord,
                   text,
                   user_type_id,
                   score
            from registration_quiz_option
            where question_id in (:questionIds)
              and is_active = true
            order by question_id asc,
                     ord asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<RegistrationQuizOption> findActiveByQuestionIds(List<Integer> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_BY_QUESTION_IDS_QUERY,
                new MapSqlParameterSource(ParameterNames.QUESTION_IDS, questionIds),
                (rs, rowNum) -> new RegistrationQuizOption(
                        rs.getInt(ColumnNames.ID),
                        rs.getInt(ColumnNames.QUESTION_ID),
                        rs.getInt(ColumnNames.ORD),
                        rs.getString(ColumnNames.TEXT),
                        rs.getInt(ColumnNames.USER_TYPE_ID),
                        rs.getInt(ColumnNames.SCORE)
                )
        );
    }
}
