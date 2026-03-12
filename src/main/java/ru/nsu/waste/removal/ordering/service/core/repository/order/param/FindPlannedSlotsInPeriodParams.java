package ru.nsu.waste.removal.ordering.service.core.repository.order.param;

import java.time.OffsetDateTime;

public record FindPlannedSlotsInPeriodParams(
        long userId,
        String postalCode,
        OffsetDateTime from,
        OffsetDateTime to
) {
}
