package ru.nsu.waste.removal.ordering.service.core.model.user;

import java.time.OffsetDateTime;
import java.util.List;

public record UserRegistrationResult(
        long userId,
        String userTypeName,
        Balance balances,
        MotivationBlock motivationBlock,
        List<EcoTask> ecoTasks,
        List<Achievement> achievements,
        List<InfoCard> infoCards
) {

    public record Balance(
            long totalPoints,
            long currentPoints
    ) {
    }

    public record MotivationBlock(
            String typeName,
            Integer currentLevelRequiredPoints,
            Integer progressPercentToNextLevel,
            String postalCode,
            String message
    ) {
    }

    public record EcoTask(
            long id,
            String title,
            String description,
            long points,
            OffsetDateTime expiredAt
    ) {
    }

    public record Achievement(
            long id,
            String title,
            String description
    ) {
    }

    public record InfoCard(
            long id,
            String title,
            String description
    ) {
    }
}
