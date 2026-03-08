package ru.nsu.waste.removal.ordering.service.core.model.courier;

import java.time.OffsetDateTime;
import java.util.List;

public record CourierOrderInfo(
        long orderId,
        OffsetDateTime orderCreatedAt,
        long userId,
        String postalCode,
        String city,
        String detailedAddress,
        String type,
        String status,
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        boolean greenChosen,
        List<String> fractions
) {
}
