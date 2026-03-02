package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderCreateParams;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderCreateRepository {

    private static final String CREATE_ORDER_QUERY = """
            insert into order_info(
                                   user_id,
                                   courier_id,
                                   type,
                                   status,
                                   pickup_from,
                                   pickup_to,
                                   green_chosen,
                                   postal_code,
                                   cost_points
                                   )
            values (
                    :userId,
                    null,
                    :type,
                    'NEW',
                    :pickupFrom,
                    :pickupTo,
                    :greenChosen,
                    :postalCode,
                    :costPoints
                    )
            returning id, created_at
            """;

    private static final String ADD_FRACTION_QUERY = """
            insert into order_waste_fraction(
                                             order_id,
                                             order_created_at,
                                             fraction_id
                                             )
            values (
                    :orderId,
                    :orderCreatedAt,
                    :fractionId
                    )
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public OrderKey createOrder(OrderCreateParams params) {
        OrderKey orderKey = namedParameterJdbcTemplate.queryForObject(
                CREATE_ORDER_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, params.userId())
                        .addValue(ParameterNames.TYPE, params.type())
                        .addValue(ParameterNames.PICKUP_FROM, params.pickupFrom())
                        .addValue(ParameterNames.PICKUP_TO, params.pickupTo())
                        .addValue(ParameterNames.GREEN_CHOSEN, params.greenChosen())
                        .addValue(ParameterNames.POSTAL_CODE, params.postalCode())
                        .addValue(ParameterNames.COST_POINTS, params.costPoints()),
                (rs, rowNum) -> new OrderKey(
                        rs.getLong(ColumnNames.ID),
                        rs.getObject(ColumnNames.CREATED_AT, OffsetDateTime.class)
                )
        );

        if (orderKey == null) {
            throw new IllegalStateException("Failed to create order");
        }

        return orderKey;
    }

    public void addFractions(OrderKey orderKey, List<Long> fractionIds) {
        if (fractionIds == null || fractionIds.isEmpty()) {
            return;
        }

        MapSqlParameterSource[] params = fractionIds.stream()
                .map(fractionId -> new MapSqlParameterSource()
                        .addValue(ParameterNames.ORDER_ID, orderKey.id())
                        .addValue(ParameterNames.ORDER_CREATED_AT, orderKey.createdAt())
                        .addValue(ParameterNames.FRACTION_ID, fractionId))
                .toArray(MapSqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(ADD_FRACTION_QUERY, params);
    }
}
