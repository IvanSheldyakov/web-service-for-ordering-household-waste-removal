package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GreenSlotRepository {

    private static final String FIND_PLANNED_SLOTS_IN_PERIOD_QUERY = """
            select distinct
                   oi.pickup_from,
                   oi.pickup_to
            from order_info oi
            where oi.postal_code = :postalCode
              and oi.user_id <> :userId
              and oi.status in ('NEW', 'ASSIGNED')
              and oi.pickup_from >= :from
              and oi.pickup_from < :to
            order by oi.pickup_from, oi.pickup_to
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<GreenSlot> findPlannedSlotsInPeriod(
            long userId,
            String postalCode,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return namedParameterJdbcTemplate.query(
                FIND_PLANNED_SLOTS_IN_PERIOD_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.POSTAL_CODE, postalCode)
                        .addValue(ParameterNames.FROM, from)
                        .addValue(ParameterNames.TO, to),
                (rs, rowNum) -> new GreenSlot(
                        rs.getObject(ColumnNames.PICKUP_FROM, OffsetDateTime.class),
                        rs.getObject(ColumnNames.PICKUP_TO, OffsetDateTime.class)
                )
        );
    }
}
