package ru.nsu.waste.removal.ordering.service.app.view;

import java.time.OffsetDateTime;
import java.util.List;

public record CourierPanelViewModel(
        long courierId,
        String fullName,
        String postalCode,
        long totalPoints,
        List<CourierOrderGroupViewModel> availableOrderGroups,
        List<CourierOrderGroupViewModel> assignedOrderGroups
) {

    public record CourierOrderGroupViewModel(
            String clusterKey,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            int ordersCount,
            int separateOrdersCount,
            int mixedOrdersCount,
            String takeActionLabel,
            List<CourierOrderViewModel> orders
    ) {
    }

    public record CourierOrderViewModel(
            long orderId,
            OffsetDateTime orderCreatedAt,
            String city,
            String detailedAddress,
            String postalCode,
            String type,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            boolean greenChosen,
            List<String> fractions
    ) {
    }
}
