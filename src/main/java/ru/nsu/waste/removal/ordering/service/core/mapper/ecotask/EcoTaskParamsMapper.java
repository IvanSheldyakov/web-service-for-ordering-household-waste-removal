package ru.nsu.waste.removal.ordering.service.core.mapper.ecotask;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.param.AddAssignedParams;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface EcoTaskParamsMapper {

    AddAssignedParams mapToAddAssignedParams(
            long userId,
            int ecoTaskId,
            OffsetDateTime assignedAt,
            OffsetDateTime expiredAt
    );
}
