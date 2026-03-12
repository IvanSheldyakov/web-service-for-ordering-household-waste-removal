package ru.nsu.waste.removal.ordering.service.core.service.order.param;

import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;

import java.util.List;

public record PayForOrderWithPointsParams(
        long userId,
        UserRewardState lockedRewardState,
        OrderKey orderKey,
        OrderType type,
        SlotOption slot,
        List<Long> fractionIds,
        long costPoints
) {
}
