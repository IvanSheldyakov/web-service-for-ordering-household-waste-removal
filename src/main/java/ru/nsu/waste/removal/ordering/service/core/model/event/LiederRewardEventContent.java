package ru.nsu.waste.removal.ordering.service.core.model.event;

public record LiederRewardEventContent(
        String algo,
        double alpha,
        double theta,
        int maxPoints,
        boolean success,
        double oldStrength,
        double newStrength,
        double fValue,
        long calculatedPoints,
        long appliedPoints
) {
}
