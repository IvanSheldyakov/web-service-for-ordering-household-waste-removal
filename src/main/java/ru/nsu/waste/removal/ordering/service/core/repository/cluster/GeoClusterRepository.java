package ru.nsu.waste.removal.ordering.service.core.repository.cluster;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterContext;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GeoClusterRepository {

    private static final String FIND_USER_CLUSTER_CONTEXT_QUERY = """
            select a.postal_code,
                   a.timezone
            from user_info ui
                     join address a on a.id = ui.address_id
            where ui.id = :userId
            """;

    private static final String FIND_PLANNED_SLOTS_IN_CLUSTER_QUERY = """
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

    private static final String COUNT_ACTIVE_ORDERS_IN_CLUSTER_QUERY = """
            select count(*)
            from order_info oi
            where oi.postal_code = :postalCode
              and oi.status in ('NEW', 'ASSIGNED')
              and oi.pickup_from >= :from
              and oi.pickup_from < :to
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public Optional<GeoClusterContext> findUserClusterContext(long userId) {
        return namedParameterJdbcTemplate.query(
                FIND_USER_CLUSTER_CONTEXT_QUERY,
                new MapSqlParameterSource(ParameterNames.USER_ID, userId),
                (rs, rowNum) -> new GeoClusterContext(
                        new GeoClusterKey(rs.getString(ColumnNames.POSTAL_CODE)),
                        rs.getString(ColumnNames.TIMEZONE)
                )
        ).stream().findFirst();
    }

    public List<GreenSlot> findPlannedSlotsInCluster(
            long excludedUserId,
            GeoClusterKey clusterKey,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return namedParameterJdbcTemplate.query(
                FIND_PLANNED_SLOTS_IN_CLUSTER_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, excludedUserId)
                        .addValue(ParameterNames.POSTAL_CODE, clusterKey.value())
                        .addValue(ParameterNames.FROM, from)
                        .addValue(ParameterNames.TO, to),
                (rs, rowNum) -> new GreenSlot(
                        rs.getObject(ColumnNames.PICKUP_FROM, OffsetDateTime.class),
                        rs.getObject(ColumnNames.PICKUP_TO, OffsetDateTime.class)
                )
        );
    }

    public long countActiveOrdersInCluster(
            GeoClusterKey clusterKey,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        Long count = namedParameterJdbcTemplate.queryForObject(
                COUNT_ACTIVE_ORDERS_IN_CLUSTER_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.POSTAL_CODE, clusterKey.value())
                        .addValue(ParameterNames.FROM, from)
                        .addValue(ParameterNames.TO, to),
                Long.class
        );
        return count == null ? 0L : count;
    }
}
