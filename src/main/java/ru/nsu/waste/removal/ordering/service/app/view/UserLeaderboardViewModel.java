package ru.nsu.waste.removal.ordering.service.app.view;

import java.util.List;

public record UserLeaderboardViewModel(
        long userId,
        String periodKey,
        String periodTitle,
        List<PeriodOptionViewModel> periodOptions,
        List<EntryViewModel> entries,
        EntryViewModel currentUserOutsideTop
) {

    public record PeriodOptionViewModel(
            String key,
            String title,
            boolean selected
    ) {
    }

    public record EntryViewModel(
            long userId,
            String displayName,
            int rankPosition,
            long score,
            boolean currentUser
    ) {
    }
}
