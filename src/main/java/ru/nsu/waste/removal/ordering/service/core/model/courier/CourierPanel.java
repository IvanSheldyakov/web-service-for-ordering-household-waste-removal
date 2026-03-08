package ru.nsu.waste.removal.ordering.service.core.model.courier;

import java.util.List;

public record CourierPanel(
        long courierId,
        String fullName,
        String postalCode,
        long totalPoints,
        List<CourierOrderInfo> availableOrders,
        List<CourierOrderInfo> assignedOrders
) {
}
