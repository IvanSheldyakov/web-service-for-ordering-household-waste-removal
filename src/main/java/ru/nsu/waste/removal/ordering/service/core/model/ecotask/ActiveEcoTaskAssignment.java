package ru.nsu.waste.removal.ordering.service.core.model.ecotask;

import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;

import java.time.OffsetDateTime;

public record ActiveEcoTaskAssignment(
        long userEcoTaskId,
        int ecoTaskId,
        String ecoTaskCode,
        long points,
        EcoTaskRuleType ruleType,
        long target,
        String orderTypeFilter,
        String orderStatusFilter,
        Boolean greenChosenFilter,
        UserActionEventType actionEventTypeFilter,
        OffsetDateTime assignedAt,
        OffsetDateTime expiredAt
) {
}
