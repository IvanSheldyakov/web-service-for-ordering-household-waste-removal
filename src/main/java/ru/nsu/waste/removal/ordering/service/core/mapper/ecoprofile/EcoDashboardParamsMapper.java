package ru.nsu.waste.removal.ordering.service.core.mapper.ecoprofile;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param.BuildDoneFiltersParams;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param.BuildInsightsParams;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface EcoDashboardParamsMapper {

    BuildInsightsParams mapToBuildInsightsParams(
            long userId,
            EcoDashboardPeriod period,
            OffsetDateTime now,
            EcoDashboard.OrdersStats currentStats
    );

    BuildDoneFiltersParams mapToBuildDoneFiltersParams(
            long userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String type
    );
}
