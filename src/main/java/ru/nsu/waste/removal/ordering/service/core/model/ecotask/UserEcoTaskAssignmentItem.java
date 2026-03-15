package ru.nsu.waste.removal.ordering.service.core.model.ecotask;

import java.time.OffsetDateTime;

public record UserEcoTaskAssignmentItem(
        long userEcoTaskId,
        long ecoTaskId,
        String title,
        String description,
        long points,
        EcoTaskAssignmentStatus status,
        OffsetDateTime expiredAt,
        OffsetDateTime completedAt
) {
}
