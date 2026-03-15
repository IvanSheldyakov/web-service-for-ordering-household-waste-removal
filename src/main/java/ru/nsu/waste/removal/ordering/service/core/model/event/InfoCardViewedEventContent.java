package ru.nsu.waste.removal.ordering.service.core.model.event;

public record InfoCardViewedEventContent(
        long cardId,
        String title
) {
}
