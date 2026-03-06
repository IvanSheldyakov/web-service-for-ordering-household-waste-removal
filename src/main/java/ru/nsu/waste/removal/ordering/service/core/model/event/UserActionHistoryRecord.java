package ru.nsu.waste.removal.ordering.service.core.model.event;

import java.time.OffsetDateTime;

public record UserActionHistoryRecord(
        long id,
        long userId,
        OffsetDateTime createdAt,
        String eventType,
        long pointsDifference,
        String content
) {
}
