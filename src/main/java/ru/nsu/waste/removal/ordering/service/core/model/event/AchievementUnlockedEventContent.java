package ru.nsu.waste.removal.ordering.service.core.model.event;

public record AchievementUnlockedEventContent(
        int achievementId,
        String achievementCode,
        String achievementTitle
) {
}
