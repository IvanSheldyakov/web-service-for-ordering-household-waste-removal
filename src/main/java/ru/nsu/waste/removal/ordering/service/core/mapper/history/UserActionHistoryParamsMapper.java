package ru.nsu.waste.removal.ordering.service.core.mapper.history;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.AddEventParams;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.CountByUserIdAndEventTypeInPeriodParams;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface UserActionHistoryParamsMapper {

    AddEventParams mapToAddEventParams(
            long userId,
            String eventType,
            String contentJson,
            long pointsDifference
    );

    CountByUserIdAndEventTypeInPeriodParams mapToCountByUserIdAndEventTypeInPeriodParams(
            long userId,
            String eventType,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
