package ru.nsu.waste.removal.ordering.service.core.service.order.param;

import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;

import java.util.List;

public record OrderCreatedEventParams(
        long userId,
        OrderKey orderKey,
        OrderType type,
        SlotOption slot,
        List<Long> fractionIds
) {
}
