package ru.nsu.waste.removal.ordering.service.core.repository.user.param;

public record UpdateRewardStateParams(
        long userId,
        long totalPoints,
        long currentPoints,
        long habitStrength
) {
}
