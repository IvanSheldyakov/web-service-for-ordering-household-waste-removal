package ru.nsu.waste.removal.ordering.service.core.model.ecoprofile;

import java.time.OffsetDateTime;

public record UserHistoryItem(
        OffsetDateTime occurredAt,
        String description,
        long pointsDelta,
        long balanceAfter
) {
}
