package ru.nsu.waste.removal.ordering.service.core.repository.courier;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.sql.Array;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourierOrderRepository {

    private static final String FIND_AVAILABLE_ORDERS_BY_COURIER_ID_QUERY = """
            select oi.id,
                   oi.created_at,
                   oi.user_id,
                   oi.postal_code,
                   a.city,
                   a.detailed_address,
                   oi.type,
                   oi.status,
                   oi.pickup_from,
                   oi.pickup_to,
                   oi.green_chosen,
                   coalesce(
                           array_agg(wf.name order by wf.name) filter ( where wf.name is not null ),
                           '{}'::text[]
                   ) as fractions
            from order_info oi
                     join courier c on c.id = :courierId
                     join user_info ui on ui.id = oi.user_id
                     join address a on a.id = ui.address_id
                     left join order_waste_fraction owf
                               on owf.order_id = oi.id
                                   and owf.order_created_at = oi.created_at
                     left join waste_fraction wf on wf.id = owf.fraction_id
            where oi.status = 'NEW'
              and oi.courier_id is null
              and oi.postal_code = c.postal_code
            group by oi.id,
                     oi.created_at,
                     oi.user_id,
                     oi.postal_code,
                     a.city,
                     a.detailed_address,
                     oi.type,
                     oi.status,
                     oi.pickup_from,
                     oi.pickup_to,
                     oi.green_chosen
            order by oi.pickup_from asc, oi.created_at asc
            limit :limit
            """;

    private static final String FIND_ASSIGNED_ORDERS_BY_COURIER_ID_QUERY = """
            select oi.id,
                   oi.created_at,
                   oi.user_id,
                   oi.postal_code,
                   a.city,
                   a.detailed_address,
                   oi.type,
                   oi.status,
                   oi.pickup_from,
                   oi.pickup_to,
                   oi.green_chosen,
                   coalesce(
                           array_agg(wf.name order by wf.name) filter ( where wf.name is not null ),
                           '{}'::text[]
                   ) as fractions
            from order_info oi
                     join user_info ui on ui.id = oi.user_id
                     join address a on a.id = ui.address_id
                     left join order_waste_fraction owf
                               on owf.order_id = oi.id
                                   and owf.order_created_at = oi.created_at
                     left join waste_fraction wf on wf.id = owf.fraction_id
            where oi.status = 'ASSIGNED'
              and oi.courier_id = :courierId
            group by oi.id,
                     oi.created_at,
                     oi.user_id,
                     oi.postal_code,
                     a.city,
                     a.detailed_address,
                     oi.type,
                     oi.status,
                     oi.pickup_from,
                     oi.pickup_to,
                     oi.green_chosen
            order by oi.pickup_from asc, oi.created_at asc
            limit :limit
            """;

    private static final String TAKE_ORDER_QUERY = """
            update order_info
            set courier_id = :courierId,
                assigned_at = :assignedAt,
                status = 'ASSIGNED'
            where id = :orderId
              and created_at = :orderCreatedAt
              and status = 'NEW'
              and courier_id is null
              and postal_code = :courierPostalCode
            """;

    private static final String FIND_ASSIGNED_ORDER_FOR_COMPLETION_QUERY = """
            select oi.id,
                   oi.created_at,
                   oi.user_id,
                   oi.postal_code,
                   a.city,
                   a.detailed_address,
                   oi.type,
                   oi.status,
                   oi.pickup_from,
                   oi.pickup_to,
                   oi.green_chosen,
                   coalesce(
                           array_agg(wf.name order by wf.name) filter ( where wf.name is not null ),
                           '{}'::text[]
                   ) as fractions
            from order_info oi
                     join user_info ui on ui.id = oi.user_id
                     join address a on a.id = ui.address_id
                     left join order_waste_fraction owf
                               on owf.order_id = oi.id
                                   and owf.order_created_at = oi.created_at
                     left join waste_fraction wf on wf.id = owf.fraction_id
            where oi.id = :orderId
              and oi.created_at = :orderCreatedAt
              and oi.courier_id = :courierId
              and oi.status = 'ASSIGNED'
            group by oi.id,
                     oi.created_at,
                     oi.user_id,
                     oi.postal_code,
                     a.city,
                     a.detailed_address,
                     oi.type,
                     oi.status,
                     oi.pickup_from,
                     oi.pickup_to,
                     oi.green_chosen
            """;

    private static final String MARK_DONE_QUERY = """
            update order_info
            set status = 'DONE',
                completed_at = :completedAt
            where id = :orderId
              and created_at = :orderCreatedAt
              and courier_id = :courierId
              and status = 'ASSIGNED'
            """;

    private static final String FIND_FRACTION_IDS_QUERY = """
            select fraction_id
            from order_waste_fraction
            where order_id = :orderId
              and order_created_at = :orderCreatedAt
            order by fraction_id asc
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public List<CourierOrderInfo> findAvailableOrdersByCourierId(long courierId, int limit) {
        return namedParameterJdbcTemplate.query(
                FIND_AVAILABLE_ORDERS_BY_COURIER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> mapCourierOrderInfo(rs)
        );
    }

    public List<CourierOrderInfo> findAssignedOrdersByCourierId(long courierId, int limit) {
        return namedParameterJdbcTemplate.query(
                FIND_ASSIGNED_ORDERS_BY_COURIER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> mapCourierOrderInfo(rs)
        );
    }

    public boolean takeOrder(
            long courierId,
            long orderId,
            OffsetDateTime orderCreatedAt,
            String courierPostalCode,
            OffsetDateTime assignedAt
    ) {
        int updatedRows = namedParameterJdbcTemplate.update(
                TAKE_ORDER_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.ORDER_ID, orderId)
                        .addValue(ParameterNames.ORDER_CREATED_AT, orderCreatedAt)
                        .addValue(ParameterNames.COURIER_POSTAL_CODE, courierPostalCode)
                        .addValue(ParameterNames.ASSIGNED_AT, assignedAt)
        );
        return updatedRows == 1;
    }

    public Optional<CourierOrderInfo> findAssignedOrderForCompletion(
            long courierId,
            long orderId,
            OffsetDateTime orderCreatedAt
    ) {
        return namedParameterJdbcTemplate.query(
                FIND_ASSIGNED_ORDER_FOR_COMPLETION_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.ORDER_ID, orderId)
                        .addValue(ParameterNames.ORDER_CREATED_AT, orderCreatedAt),
                (rs, rowNum) -> mapCourierOrderInfo(rs)
        ).stream().findFirst();
    }

    public boolean markDone(
            long courierId,
            long orderId,
            OffsetDateTime orderCreatedAt,
            OffsetDateTime completedAt
    ) {
        int updatedRows = namedParameterJdbcTemplate.update(
                MARK_DONE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.ORDER_ID, orderId)
                        .addValue(ParameterNames.ORDER_CREATED_AT, orderCreatedAt)
                        .addValue(ParameterNames.COMPLETED_AT, completedAt)
        );
        return updatedRows == 1;
    }

    public List<Long> findFractionIds(long orderId, OffsetDateTime orderCreatedAt) {
        return namedParameterJdbcTemplate.query(
                FIND_FRACTION_IDS_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.ORDER_ID, orderId)
                        .addValue(ParameterNames.ORDER_CREATED_AT, orderCreatedAt),
                (rs, rowNum) -> rs.getLong(ColumnNames.FRACTION_ID)
        );
    }

    private CourierOrderInfo mapCourierOrderInfo(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        Array fractionsArray = resultSet.getArray(ColumnNames.FRACTIONS);
        List<String> fractions = extractTextArray(fractionsArray);

        return new CourierOrderInfo(
                resultSet.getLong(ColumnNames.ID),
                resultSet.getObject(ColumnNames.CREATED_AT, OffsetDateTime.class),
                resultSet.getLong(ColumnNames.USER_ID),
                resultSet.getString(ColumnNames.POSTAL_CODE),
                resultSet.getString(ColumnNames.CITY),
                resultSet.getString(ColumnNames.DETAILED_ADDRESS),
                resultSet.getString(ColumnNames.TYPE),
                resultSet.getString(ColumnNames.STATUS),
                resultSet.getObject(ColumnNames.PICKUP_FROM, OffsetDateTime.class),
                resultSet.getObject(ColumnNames.PICKUP_TO, OffsetDateTime.class),
                resultSet.getBoolean(ColumnNames.GREEN_CHOSEN),
                fractions
        );
    }

    private List<String> extractTextArray(Array sqlArray) throws java.sql.SQLException {
        if (sqlArray == null) {
            return List.of();
        }

        Object rawArray = sqlArray.getArray();
        sqlArray.free();

        if (rawArray instanceof String[] value) {
            return Arrays.stream(value)
                    .filter(Objects::nonNull)
                    .toList();
        }
        if (rawArray instanceof Object[] value) {
            return Arrays.stream(value)
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }
}
