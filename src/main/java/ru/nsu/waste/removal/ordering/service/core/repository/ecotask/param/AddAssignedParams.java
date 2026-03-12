package ru.nsu.waste.removal.ordering.service.core.repository.ecotask.param;

import java.time.OffsetDateTime;

public record AddAssignedParams(
        long userId,
        int ecoTaskId,
        OffsetDateTime assignedAt,
        OffsetDateTime expiredAt
) {
}
