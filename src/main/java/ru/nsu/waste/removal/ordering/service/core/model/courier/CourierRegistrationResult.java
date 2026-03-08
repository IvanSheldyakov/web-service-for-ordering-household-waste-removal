package ru.nsu.waste.removal.ordering.service.core.model.courier;

public record CourierRegistrationResult(
        long courierId,
        String fullName,
        String postalCode,
        long totalPoints
) {
}
