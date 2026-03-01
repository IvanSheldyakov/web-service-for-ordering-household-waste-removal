package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

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

    public long countOrdersByFiltersInPeriod(OrderFiltersInPeriod filters) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_ORDERS_BY_FILTERS_IN_PERIOD_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, filters.userId())
                        .addValue(ParameterNames.FROM, filters.from())
                        .addValue(ParameterNames.TO, filters.to())
                        .addValue(ParameterNames.STATUS, filters.status())
                        .addValue(ParameterNames.TYPE, filters.type())
                        .addValue(ParameterNames.GREEN_CHOSEN, filters.greenChosen()),
                Long.class
        );
        return count == null ? 0L : count;
    }

    public long countDistinctFractionsByFiltersInPeriod(OrderFiltersInPeriod filters) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_DISTINCT_FRACTIONS_BY_FILTERS_IN_PERIOD_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, filters.userId())
                        .addValue(ParameterNames.FROM, filters.from())
                        .addValue(ParameterNames.TO, filters.to())
                        .addValue(ParameterNames.STATUS, filters.status())
                        .addValue(ParameterNames.TYPE, filters.type())
                        .addValue(ParameterNames.GREEN_CHOSEN, filters.greenChosen()),
                Long.class
        );
        return count == null ? 0L : count;
    }
}
