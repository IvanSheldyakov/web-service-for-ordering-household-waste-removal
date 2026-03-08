package ru.nsu.waste.removal.ordering.service.core.service.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderCreateParams;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;

@Mapper(componentModel = "spring")
public interface OrderCreateParamsMapper {

    @Mapping(target = "pickupFrom", source = "selectedSlot.pickupFrom")
    @Mapping(target = "pickupTo", source = "selectedSlot.pickupTo")
    @Mapping(target = "greenChosen", source = "selectedSlot.green")
    @Mapping(target = "clusterKey", source = "clusterKey.value")
    @Mapping(target = "costPoints", source = "costPoints")
    OrderCreateParams toOrderCreateParams(
            long userId,
            OrderType type,
            SlotOption selectedSlot,
            GeoClusterKey clusterKey,
            long costPoints
    );
}
