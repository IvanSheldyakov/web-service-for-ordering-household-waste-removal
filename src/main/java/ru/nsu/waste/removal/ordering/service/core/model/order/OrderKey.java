package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;

public record OrderKey(
        long id,
        OffsetDateTime createdAt
) {
}
