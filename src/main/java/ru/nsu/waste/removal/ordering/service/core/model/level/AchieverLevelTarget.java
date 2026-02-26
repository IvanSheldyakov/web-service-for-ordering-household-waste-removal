package ru.nsu.waste.removal.ordering.service.core.model.level;

public record AchieverLevelTarget(
        long userId,
        int levelId,
        int requiredTotalPoints
) {
}
