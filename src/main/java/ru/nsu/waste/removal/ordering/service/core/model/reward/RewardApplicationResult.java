package ru.nsu.waste.removal.ordering.service.core.model.reward;

import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;

public record RewardApplicationResult(
        long userId,
        UserActionEventType eventType,
        boolean success,
        long appliedPointsDelta,
        int calculatedPointsDelta,
        long newTotalPoints,
        long newCurrentPoints,
        double newHabitStrength
) {
}
