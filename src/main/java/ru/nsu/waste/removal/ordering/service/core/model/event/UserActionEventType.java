package ru.nsu.waste.removal.ordering.service.core.model.event;

import java.util.Locale;

public enum UserActionEventType {
    ORDER_DONE,
    ORDER_CREATED,
    SEPARATE_CHOSEN,
    GREEN_SLOT_CHOSEN,
    LEVEL_UP,
    LEADERBOARD_OPENED,
    ECO_PROFILE_OPENED,
    INFO_CARD_VIEWED,
    ACHIEVEMENT_UNLOCKED,
    ECO_TASK_COMPLETED;

    public String dbName() {
        return name();
    }

    public static UserActionEventType fromDbName(String value) {
        return UserActionEventType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
