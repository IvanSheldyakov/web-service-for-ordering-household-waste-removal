package ru.nsu.waste.removal.ordering.service.core.model.user;

import java.util.List;

public record UserLeaderboard(
        long userId,
        LeaderboardPeriod period,
        List<UserLeaderboardEntry> topEntries,
        UserLeaderboardEntry currentUserEntry,
        int topLimit
) {
}
