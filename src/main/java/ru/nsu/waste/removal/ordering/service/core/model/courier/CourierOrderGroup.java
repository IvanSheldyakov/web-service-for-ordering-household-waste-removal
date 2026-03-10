package ru.nsu.waste.removal.ordering.service.core.model.courier;

import java.time.OffsetDateTime;
import java.util.List;

public record CourierOrderGroup(
        String clusterKey,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        int ordersCount,
        int separateOrdersCount,
        int mixedOrdersCount,
        List<CourierOrderInfo> orders
) {
}
