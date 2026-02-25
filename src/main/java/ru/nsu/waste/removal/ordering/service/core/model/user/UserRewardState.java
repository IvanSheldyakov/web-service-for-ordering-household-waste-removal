package ru.nsu.waste.removal.ordering.service.core.model.user;

public record UserRewardState(
        long userId,
        long totalPoints,
        long currentPoints,
        long habitStrength
) {
}
