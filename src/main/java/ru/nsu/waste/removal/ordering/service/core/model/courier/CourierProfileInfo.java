package ru.nsu.waste.removal.ordering.service.core.model.courier;

public record CourierProfileInfo(
        long courierId,
        String fullName,
        String postalCode,
        String timezone,
        long totalPoints
) {
}
