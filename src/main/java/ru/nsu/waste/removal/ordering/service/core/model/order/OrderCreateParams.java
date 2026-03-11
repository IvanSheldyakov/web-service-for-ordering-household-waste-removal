package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;

public record OrderCreateParams(
        long userId,
        OrderType type,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        boolean greenChosen,
        String clusterKey,
        long costPoints,
        OrderPaymentStatus paymentStatus
) {
}
