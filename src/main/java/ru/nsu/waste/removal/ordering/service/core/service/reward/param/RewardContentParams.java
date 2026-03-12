package ru.nsu.waste.removal.ordering.service.core.service.reward.param;

public record RewardContentParams(
        boolean success,
        double oldStrength,
        double newStrength,
        double fValue,
        long calculatedDelta,
        long appliedDelta
) {
}
