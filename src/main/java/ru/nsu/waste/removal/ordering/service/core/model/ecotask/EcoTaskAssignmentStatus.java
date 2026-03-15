package ru.nsu.waste.removal.ordering.service.core.model.ecotask;

import java.util.Locale;

public enum EcoTaskAssignmentStatus {
    ASSIGNED,
    DONE,
    EXPIRED,
    CANCELLED;

    public static EcoTaskAssignmentStatus fromDbName(String value) {
        return EcoTaskAssignmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
