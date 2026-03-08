package ru.nsu.waste.removal.ordering.service.core.model.event;

import java.util.List;

public record OrderDoneEventContent(
        long orderId,
        String type,
        boolean greenChosen,
        String pickupFrom,
        String pickupTo,
        List<Long> fractionIds,
        long courierId,
        String status
) {
}
