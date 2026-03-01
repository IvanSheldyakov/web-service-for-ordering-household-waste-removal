package ru.nsu.waste.removal.ordering.service.core.model.event;

public record UserActionHistoryEvent(
        long id,
        long userId,
        UserActionEventType eventType,
        long pointsDifference,
        String content
) {
}
