package ru.nsu.waste.removal.ordering.service.core.model.user;

public record UserGreenSlotContext(
        long userId,
        String postalCode,
        String timezone
) {
}
