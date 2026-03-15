package ru.nsu.waste.removal.ordering.service.core.repository.infocard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.List;
import java.util.Optional;

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

    private static final String FIND_BY_ID_QUERY = """
            select id,
                   title,
                   description
            from info_card
            where id = :id
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

    public Optional<InfoCard> findById(long cardId) {
        return namedParameterJdbcTemplate.query(
                FIND_BY_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.ID, cardId),
                (rs, rowNum) -> new InfoCard(
                        rs.getInt(ColumnNames.ID),
                        rs.getString(ColumnNames.TITLE),
                        rs.getString(ColumnNames.DESCRIPTION)
                )
        ).stream().findFirst();
    }
}
