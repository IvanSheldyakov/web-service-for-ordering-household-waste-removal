package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;

public record OrderFiltersInPeriod(
        long userId,
        OffsetDateTime from,
        OffsetDateTime to,
        String status,
        String type,
        Boolean greenChosen
) {
}
