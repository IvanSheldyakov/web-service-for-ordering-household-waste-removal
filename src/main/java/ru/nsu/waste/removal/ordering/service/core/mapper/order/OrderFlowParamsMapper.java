package ru.nsu.waste.removal.ordering.service.core.mapper.order;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.service.order.param.OrderCreatedEventParams;
import ru.nsu.waste.removal.ordering.service.core.service.order.param.PayForOrderWithPointsParams;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderFlowParamsMapper {

    OrderCreatedEventParams mapToOrderCreatedEventParams(
            long userId,
            OrderKey orderKey,
            OrderType type,
            SlotOption slot,
            List<Long> fractionIds
    );

    @Mapping(target = "lockedRewardState", source = "lockedRewardState")
    PayForOrderWithPointsParams mapToPayForOrderWithPointsParams(
            long userId,
            UserRewardState lockedRewardState,
            OrderKey orderKey,
            OrderType type,
            SlotOption slot,
            List<Long> fractionIds,
            long costPoints
    );
}
