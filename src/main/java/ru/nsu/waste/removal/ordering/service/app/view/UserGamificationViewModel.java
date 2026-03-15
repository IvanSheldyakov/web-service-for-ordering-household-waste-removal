package ru.nsu.waste.removal.ordering.service.app.view;

import java.time.OffsetDateTime;
import java.util.List;

public record UserGamificationViewModel(
        long userId,
        List<EcoTaskViewModel> ecoTasks,
        List<AchievementViewModel> achievements
) {

    public record EcoTaskViewModel(
            long assignmentId,
            long ecoTaskId,
            String title,
            String description,
            long points,
            String status,
            OffsetDateTime expiredAt,
            OffsetDateTime completedAt
    ) {
    }

    public record AchievementViewModel(
            int achievementId,
            String title,
            String description,
            boolean unlocked
    ) {
    }
}
