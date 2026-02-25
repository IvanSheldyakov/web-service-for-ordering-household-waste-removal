package ru.nsu.waste.removal.ordering.service.core.model.user;

public record UserProfileInfo(
        long userId,
        UserType userType,
        long totalPoints,
        long currentPoints,
        String postalCode
) {
}
