package ru.nsu.waste.removal.ordering.service.core.model.cluster;

public record GeoClusterKey(String value) {

    public GeoClusterKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cluster key value must not be blank");
        }
    }
}
