package ru.nsu.waste.removal.ordering.service.core.model.ecotask;

public record EcoTask(
        int id,
        String code,
        String title,
        String description,
        long points,
        EcoTaskPeriod period
) {
}
