package ru.nsu.waste.removal.ordering.service.core.repository.courier.param;

import java.time.OffsetDateTime;

public record MarkDoneParams(
        long courierId,
        long orderId,
        OffsetDateTime orderCreatedAt,
        OffsetDateTime completedAt
) {
}
