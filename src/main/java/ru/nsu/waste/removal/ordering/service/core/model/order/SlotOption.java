package ru.nsu.waste.removal.ordering.service.core.model.order;

import java.time.OffsetDateTime;

public record SlotOption(
        OffsetDateTime pickupFrom,
        OffsetDateTime pickupTo,
        boolean green
) {

    public String key() {
        return pickupFrom + "|" + pickupTo;
    }
}
