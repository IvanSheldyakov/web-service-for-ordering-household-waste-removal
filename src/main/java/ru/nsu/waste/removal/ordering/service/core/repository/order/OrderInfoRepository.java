package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class OrderInfoRepository {

    private static final String COUNT_DONE_ORDERS_QUERY = """
            select count(*)
            from order_info oi
            where oi.user_id = :userId
              and oi.status = 'DONE'
            """;

    private static final String COUNT_DONE_SEPARATE_ORDERS_QUERY = """
            select count(*)
            from order_info oi
            where oi.user_id = :userId
              and oi.status = 'DONE'
              and oi.type = 'SEPARATE'
            """;

    private static final String COUNT_DONE_GREEN_ORDERS_QUERY = """
            select count(*)
            from order_info oi
            where oi.user_id = :userId
              and oi.status = 'DONE'
              and oi.green_chosen = true
            """;

    private static final String COUNT_DISTINCT_FRACTIONS_IN_DONE_SEPARATE_ORDERS_QUERY = """
            select count(distinct owf.fraction_id)
            from order_info oi
                     join order_waste_fraction owf
                          on owf.order_id = oi.id
                              and owf.order_created_at = oi.created_at
            where oi.user_id = :userId
              and oi.status = 'DONE'
              and oi.type = 'SEPARATE'
            """;

    private static final String FIND_ACTIVE_ORDERS_BY_USER_ID_QUERY = """
            select oi.id,
                   oi.type,
                   oi.status,
                   oi.pickup_from,
                   oi.pickup_to,
                   coalesce(
                           array_agg(wf.name order by wf.name) filter ( where wf.name is not null ),
                           '{}'::text[]
                   ) as fractions
            from order_info oi
                     left join order_waste_fraction owf
                               on owf.order_id = oi.id
                                   and owf.order_created_at = oi.created_at
                     left join waste_fraction wf on wf.id = owf.fraction_id
            where oi.user_id = :userId
              and oi.status in ('NEW', 'ASSIGNED')
            group by oi.id, oi.created_at, oi.type, oi.status, oi.pickup_from, oi.pickup_to
            order by oi.pickup_from asc
            limit :limit
            """;

    private static final String COUNT_ORDERS_BY_FILTERS_IN_PERIOD_QUERY = """
            select count(*)
            from order_info oi
            where oi.user_id = :userId
              and oi.created_at >= :from
              and oi.created_at <= :to
              and (cast(:status as varchar) is null or oi.status = cast(:status as varchar))
              and (cast(:type as varchar) is null or oi.type = cast(:type as varchar))
              and (cast(:greenChosen as boolean) is null or oi.green_chosen = cast(:greenChosen as boolean))
            """;

    private static final String COUNT_DISTINCT_FRACTIONS_BY_FILTERS_IN_PERIOD_QUERY = """
            select count(distinct owf.fraction_id)
            from order_info oi
                     join order_waste_fraction owf
                          on owf.order_id = oi.id
                              and owf.order_created_at = oi.created_at
            where oi.user_id = :userId
              and oi.created_at >= :from
              and oi.created_at <= :to
              and (cast(:status as varchar) is null or oi.status = cast(:status as varchar))
              and (cast(:type as varchar) is null or oi.type = cast(:type as varchar))
              and (cast(:greenChosen as boolean) is null or oi.green_chosen = cast(:greenChosen as boolean))
            """;

    private static final String FIND_DISTINCT_FRACTION_NAMES_BY_FILTERS_IN_PERIOD_QUERY = """
            select distinct wf.name
            from order_info oi
                     join order_waste_fraction owf
                          on owf.order_id = oi.id
                              and owf.order_created_at = oi.created_at
                     join waste_fraction wf
                          on wf.id = owf.fraction_id
            where oi.user_id = :userId
              and oi.created_at >= :from
              and oi.created_at <= :to
              and (cast(:status as varchar) is null or oi.status = cast(:status as varchar))
              and (cast(:type as varchar) is null or oi.type = cast(:type as varchar))
              and (cast(:greenChosen as boolean) is null or oi.green_chosen = cast(:greenChosen as boolean))
            order by wf.name
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long countDoneOrders(long userId) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DONE_ORDERS_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countDoneSeparateOrders(long userId) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DONE_SEPARATE_ORDERS_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countDoneGreenOrders(long userId) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DONE_GREEN_ORDERS_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countDistinctFractionsInDoneSeparateOrders(long userId) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DISTINCT_FRACTIONS_IN_DONE_SEPARATE_ORDERS_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public List<ActiveOrderInfo> findActiveOrdersByUserId(long userId, int limit) {
        return namedParameterJdbcTemplate.query(
                FIND_ACTIVE_ORDERS_BY_USER_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.LIMIT, limit),
                (rs, rowNum) -> {
                    Array fractionsArray = rs.getArray(ColumnNames.FRACTIONS);
                    List<String> fractions;
                    if (fractionsArray == null) {
                        fractions = List.of();
                    } else {
                        Object rawArray = fractionsArray.getArray();
                        fractionsArray.free();

                        if (rawArray instanceof String[] value) {
                            fractions = Arrays.stream(value)
                                    .filter(Objects::nonNull)
                                    .toList();
                        } else if (rawArray instanceof Object[] value) {
                            fractions = Arrays.stream(value)
                                    .filter(Objects::nonNull)
                                    .map(String::valueOf)
                                    .toList();
                        } else {
                            fractions = List.of();
                        }
                    }

                    return new ActiveOrderInfo(
                            rs.getLong(ColumnNames.ID),
                            rs.getString(ColumnNames.TYPE),
                            rs.getString(ColumnNames.STATUS),
                            rs.getObject(ColumnNames.PICKUP_FROM, java.time.OffsetDateTime.class),
                            rs.getObject(ColumnNames.PICKUP_TO, java.time.OffsetDateTime.class),
                            fractions
                    );
                }
        );
    }

    public long countOrdersByFiltersInPeriod(OrderFiltersInPeriod filters) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_ORDERS_BY_FILTERS_IN_PERIOD_QUERY,
                toPeriodFiltersParams(filters),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countDistinctFractionsByFiltersInPeriod(OrderFiltersInPeriod filters) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DISTINCT_FRACTIONS_BY_FILTERS_IN_PERIOD_QUERY,
                toPeriodFiltersParams(filters),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public List<String> findDistinctFractionNamesByFiltersInPeriod(OrderFiltersInPeriod filters) {
        return namedParameterJdbcTemplate.query(
                FIND_DISTINCT_FRACTION_NAMES_BY_FILTERS_IN_PERIOD_QUERY,
                toPeriodFiltersParams(filters),
                (rs, rowNum) -> rs.getString(ColumnNames.NAME)
        );
    }

    private MapSqlParameterSource toPeriodFiltersParams(OrderFiltersInPeriod filters) {
        return new MapSqlParameterSource()
                .addValue(ParameterNames.USER_ID, filters.userId())
                .addValue(ParameterNames.FROM, filters.from())
                .addValue(ParameterNames.TO, filters.to())
                .addValue(ParameterNames.STATUS, filters.status())
                .addValue(ParameterNames.TYPE, filters.type())
                .addValue(ParameterNames.GREEN_CHOSEN, filters.greenChosen());
    }
}
