package ru.nsu.waste.removal.ordering.service.app.view;

public record CourierRegistrationResultViewModel(
        long courierId,
        String fullName,
        String postalCode,
        long totalPoints
) {
}
