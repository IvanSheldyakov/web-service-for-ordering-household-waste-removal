package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.util.Locale;
import java.util.Optional;

public enum OrderType {
    MIXED,
    SEPARATE;

    public String dbName() {
        return name();
    }

    public static Optional<OrderType> tryFrom(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(OrderType.valueOf(normalized));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
