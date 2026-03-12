package ru.nsu.waste.removal.ordering.service.core.mapper.courier;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.param.MarkDoneParams;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.param.TakeOrderGroupParams;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.param.TakeOrderParams;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface CourierParamsMapper {

    TakeOrderParams mapToTakeOrderParams(
            long courierId,
            long orderId,
            OffsetDateTime orderCreatedAt,
            String courierPostalCode,
            OffsetDateTime assignedAt
    );

    TakeOrderGroupParams mapToTakeOrderGroupParams(
            long courierId,
            String clusterKey,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            OffsetDateTime assignedAt
    );

    MarkDoneParams mapToMarkDoneParams(
            long courierId,
            long orderId,
            OffsetDateTime orderCreatedAt,
            OffsetDateTime completedAt
    );
}
