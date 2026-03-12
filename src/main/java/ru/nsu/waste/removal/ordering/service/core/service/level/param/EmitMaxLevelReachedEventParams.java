package ru.nsu.waste.removal.ordering.service.core.service.level.param;

import ru.nsu.waste.removal.ordering.service.core.model.level.Level;

public record EmitMaxLevelReachedEventParams(
        long userId,
        Level current,
        long oldTotalPoints,
        long newTotalPoints
) {
}
