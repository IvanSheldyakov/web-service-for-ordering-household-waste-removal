package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;
import java.util.List;

public record ActiveOrderInfo(
        long orderId,
        String type,
        String status,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        List<String> fractions
) {
}

