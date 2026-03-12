package ru.nsu.waste.removal.ordering.service.core.repository.cluster.param;

import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;

import java.time.OffsetDateTime;

public record FindPlannedSlotsInClusterParams(
        long excludedUserId,
        GeoClusterKey clusterKey,
        OffsetDateTime from,
        OffsetDateTime to
) {
}
