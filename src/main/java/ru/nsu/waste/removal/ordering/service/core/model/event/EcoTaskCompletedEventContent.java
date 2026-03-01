package ru.nsu.waste.removal.ordering.service.core.model.event;

public record EcoTaskCompletedEventContent(
        long userEcoTaskId,
        long ecoTaskId,
        String ecoTaskCode,
        long rewardPoints
) {
}
