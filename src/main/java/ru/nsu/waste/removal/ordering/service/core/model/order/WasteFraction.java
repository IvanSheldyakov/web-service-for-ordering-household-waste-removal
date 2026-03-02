package ru.nsu.waste.removal.ordering.service.core.model.order;

public record WasteFraction(
        long id,
        String type,
        String name,
        boolean active
) {
}
