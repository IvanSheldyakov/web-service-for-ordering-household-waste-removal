package ru.nsu.waste.removal.ordering.service.core.model.ecoprofile;

import java.util.List;

public record UserHistory(
        long userId,
        long currentPoints,
        List<UserHistoryItem> items
) {
}
