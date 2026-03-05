package ru.nsu.waste.removal.ordering.service.core.model.ecoprofile;

import java.util.Locale;

public enum EcoDashboardPeriod {
    WEEK(7, "7 дней"),
    MONTH(30, "30 дней"),
    ALL(0, "Всё время");

    private final int lengthDays;
    private final String title;

    EcoDashboardPeriod(int lengthDays, String title) {
        this.lengthDays = lengthDays;
        this.title = title;
    }

    public static EcoDashboardPeriod fromQuery(String raw) {
        if (raw == null || raw.isBlank()) {
            return WEEK;
        }

        try {
            return EcoDashboardPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
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
