package ru.nsu.waste.removal.ordering.service.core.model.event;

import java.time.OffsetDateTime;

public record UserActionHistoryEvent(
        long id,
        OffsetDateTime createdAt,
        long userId,
        UserActionEventType eventType,
        long pointsDifference,
        String content
) {
}
