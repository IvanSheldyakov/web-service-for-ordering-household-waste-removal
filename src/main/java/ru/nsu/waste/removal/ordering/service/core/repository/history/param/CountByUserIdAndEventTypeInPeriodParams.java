package ru.nsu.waste.removal.ordering.service.core.repository.history.param;

import java.time.OffsetDateTime;

public record CountByUserIdAndEventTypeInPeriodParams(
        long userId,
        String eventType,
        OffsetDateTime from,
        OffsetDateTime to
) {
}
