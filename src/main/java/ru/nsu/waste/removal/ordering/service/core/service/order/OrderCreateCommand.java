package ru.nsu.waste.removal.ordering.service.core.service.order;

import java.util.List;

public record OrderCreateCommand(
        String type,
        String slotKey,
        List<Long> fractionIds,
        boolean payWithPoints
) {
}
