package ru.nsu.waste.removal.ordering.service.core.model.user;

import java.util.Locale;

public enum UserType {
    ACHIEVER,
    SOCIALIZER,
    EXPLORER;

    public static UserType fromDbName(String value) {
        return UserType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
