package ru.nsu.waste.removal.ordering.service.core.model.event;

public record LevelUpEventContent(
        long fromLevelId,
        long toLevelId,
        long fromRequiredTotalPoints,
        long toRequiredTotalPoints,
        long oldTotalPoints,
        long newTotalPoints,
        boolean maxReached
) {
}
