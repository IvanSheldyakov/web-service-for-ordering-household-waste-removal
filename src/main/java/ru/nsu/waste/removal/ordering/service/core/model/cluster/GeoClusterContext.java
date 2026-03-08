package ru.nsu.waste.removal.ordering.service.core.model.cluster;

public record GeoClusterContext(
        GeoClusterKey clusterKey,
        String timezone
) {
}
