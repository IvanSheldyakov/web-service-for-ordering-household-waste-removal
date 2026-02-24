package ru.nsu.waste.removal.ordering.service.app.view;

import java.time.OffsetDateTime;
import java.util.List;

public record RegistrationResultViewModel(
        long userId,
        String userTypeName,
        BalanceViewModel balances,
        MotivationBlockViewModel motivationBlock,
        List<EcoTaskViewModel> ecoTasks,
        List<AchievementViewModel> achievements,
        List<InfoCardViewModel> infoCards
) {

    public record BalanceViewModel(
            long totalPoints,
            long currentPoints
    ) {
    }

    public record MotivationBlockViewModel(
            String typeName,
            Integer currentLevelRequiredPoints,
            Integer progressPercentToNextLevel,
            String postalCode,
            String message
    ) {
    }

    public record EcoTaskViewModel(
            long id,
            String title,
            String description,
            long points,
            OffsetDateTime expiredAt
    ) {
    }

    public record AchievementViewModel(
            long id,
            String title,
            String description
    ) {
    }

    public record InfoCardViewModel(
            long id,
            String title,
            String description
    ) {
    }
}
