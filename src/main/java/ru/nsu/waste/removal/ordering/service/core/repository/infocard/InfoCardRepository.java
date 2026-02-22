package ru.nsu.waste.removal.ordering.service.core.repository.infocard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class InfoCardRepository {

    private static final String FIND_RANDOM_QUERY = """
            select id,
                   title,
                   description
            from info_card
            order by random()
            limit :limit
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<InfoCard> findRandom(int limit) {
        return namedParameterJdbcTemplate.query(
                FIND_RANDOM_QUERY,
                new MapSqlParameterSource(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> new InfoCard(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION)
                )
        );
    }
}
