package ru.nsu.waste.removal.ordering.service.core.service.level.param;

import ru.nsu.waste.removal.ordering.service.core.model.level.Level;

public record LevelUpContentParams(
        Level from,
        Level to,
        long oldTotalPoints,
        long newTotalPoints,
        boolean maxReached
) {
}
