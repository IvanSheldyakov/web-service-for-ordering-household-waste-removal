package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
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
}
