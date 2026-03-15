package ru.nsu.waste.removal.ordering.service.core.model.user;

public record UserLeaderboardEntry(
        long userId,
        String fullName,
        int rankPosition,
        long score
) {
}
