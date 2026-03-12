package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param;

import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;

import java.time.OffsetDateTime;

public record BuildInsightsParams(
        long userId,
        EcoDashboardPeriod period,
        OffsetDateTime now,
        EcoDashboard.OrdersStats currentStats
) {
}
