package ru.nsu.waste.removal.ordering.service.core.model.event;

import java.util.List;

public record OrderCreatedEventContent(
        long orderId,
        String type,
        String pickupFrom,
        String pickupTo,
        boolean greenChosen,
        List<Long> fractionIds
) {
}
