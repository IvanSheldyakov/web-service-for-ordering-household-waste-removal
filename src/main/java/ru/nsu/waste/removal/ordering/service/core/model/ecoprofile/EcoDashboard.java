package ru.nsu.waste.removal.ordering.service.core.model.ecoprofile;

import java.util.List;

public record EcoDashboard(
        long userId,
        String userType,
        long currentPoints,
        long totalPoints,
        Period period,
        OrdersStats orders,
        List<String> fractions,
        List<String> insights,
        AchieverProgress achiever
) {

    public record Period(
            String key,
            String title,
            List<PeriodOption> options
    ) {
    }

    public record PeriodOption(
            String key,
            String title,
            boolean selected
    ) {
    }

    public record OrdersStats(
            long doneTotal,
            long doneSeparate,
            int separateSharePercent
    ) {
    }

    public record AchieverProgress(
            int levelId,
            Long nextRequiredPoints,
            long remaining,
            int progressPercent,
            boolean maxLevelReached
    ) {
    }
}
