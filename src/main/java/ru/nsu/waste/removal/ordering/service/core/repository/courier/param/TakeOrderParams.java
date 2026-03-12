package ru.nsu.waste.removal.ordering.service.core.repository.courier.param;

import java.time.OffsetDateTime;

public record TakeOrderParams(
        long courierId,
        long orderId,
        OffsetDateTime orderCreatedAt,
        String courierPostalCode,
        OffsetDateTime assignedAt
) {
}
