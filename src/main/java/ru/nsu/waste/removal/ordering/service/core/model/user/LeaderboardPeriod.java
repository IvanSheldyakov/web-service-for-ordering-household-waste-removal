package ru.nsu.waste.removal.ordering.service.core.model.user;

import java.util.Locale;

public enum LeaderboardPeriod {
    WEEK(7, "7 дней"),
    MONTH(30, "30 дней");

    private final int lengthDays;
    private final String title;

    LeaderboardPeriod(int lengthDays, String title) {
        this.lengthDays = lengthDays;
        this.title = title;
    }

    public static LeaderboardPeriod fromQuery(String raw) {
        if (raw == null || raw.isBlank()) {
            return WEEK;
        }

        try {
            return LeaderboardPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return WEEK;
        }
    }

    public int lengthDays() {
        return lengthDays;
    }

    public String title() {
        return title;
    }
}
