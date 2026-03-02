package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;

public record OrderCreateParams(
        long userId,
        String type,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        boolean greenChosen,
        String postalCode,
        long costPoints
) {
}
