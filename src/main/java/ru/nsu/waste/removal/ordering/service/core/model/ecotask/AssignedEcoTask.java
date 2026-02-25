package ru.nsu.waste.removal.ordering.service.core.model.ecotask;

import java.time.OffsetDateTime;

public record AssignedEcoTask(
        long id,
        long ecoTaskId,
        String title,
        String description,
        long points,
        OffsetDateTime expiredAt
) {
}
