package ru.nsu.waste.removal.ordering.service.app.view;

import java.time.OffsetDateTime;
import java.util.List;

public record UserHistoryViewModel(
        long userId,
        long currentPoints,
        List<ItemViewModel> items
) {

    public record ItemViewModel(
            OffsetDateTime occurredAt,
            String description,
            long pointsDelta,
            long balanceAfter
    ) {
    }
}
