package ru.nsu.waste.removal.ordering.service.core.model.courier;

import java.time.OffsetDateTime;

public record CourierOrderGroupKey(
        String clusterKey,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo
) {
}
