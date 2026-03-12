package ru.nsu.waste.removal.ordering.service.core.repository.history.param;

public record AddEventParams(
        long userId,
        String eventType,
        String contentJson,
        long pointsDifference
) {
}
