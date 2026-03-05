package ru.nsu.waste.removal.ordering.service.app.view;

import java.util.List;

public record EcoDashboardViewModel(
        long userId,
        String userType,
        long currentPoints,
        long totalPoints,
        PeriodViewModel period,
        OrdersStatsViewModel orders,
        List<String> fractions,
        List<String> insights,
        AchieverProgressViewModel achiever
) {

    public record PeriodViewModel(
            String key,
            String title,
            List<PeriodOptionViewModel> options
    ) {
    }

    public record PeriodOptionViewModel(
            String key,
            String title,
            boolean selected
    ) {
    }

    public record OrdersStatsViewModel(
            long doneTotal,
            long doneSeparate,
            int separateSharePercent
    ) {
    }

    public record AchieverProgressViewModel(
            int levelId,
            Long nextRequiredPoints,
            long remaining,
            int progressPercent,
            boolean maxLevelReached
    ) {
    }
}
