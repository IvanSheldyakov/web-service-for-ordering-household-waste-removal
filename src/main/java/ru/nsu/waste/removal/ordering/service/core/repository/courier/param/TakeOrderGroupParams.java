package ru.nsu.waste.removal.ordering.service.core.repository.courier.param;

import java.time.OffsetDateTime;

public record TakeOrderGroupParams(
        long courierId,
        String clusterKey,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        OffsetDateTime assignedAt
) {
}
