package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param;

import java.time.OffsetDateTime;

public record BuildDoneFiltersParams(
        long userId,
        OffsetDateTime from,
        OffsetDateTime to,
        String type
) {
}
