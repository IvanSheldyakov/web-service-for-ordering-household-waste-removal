package ru.nsu.waste.removal.ordering.service.core.service.cluster;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterContext;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.repository.cluster.GeoClusterRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.cluster.param.FindPlannedSlotsInClusterParams;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GeoClusterService {

    private final GeoClusterRepository geoClusterRepository;

    public GeoClusterContext getUserClusterContext(long userId) {
        return geoClusterRepository.findUserClusterContext(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User cluster context is not found for user id = %s".formatted(userId)
                ));
    }

    public GeoClusterKey getClusterKeyByUserId(long userId) {
        return getUserClusterContext(userId).clusterKey();
    }

    public boolean belongsToSameCluster(GeoClusterKey left, GeoClusterKey right) {
        return left != null
                && right != null
                && Objects.equals(left.value(), right.value());
    }

    public GeoClusterKey getClusterKeyForOrderCreation(long userId) {
        return getClusterKeyByUserId(userId);
    }

    public List<GreenSlot> findPlannedSlotsInCluster(
            FindPlannedSlotsInClusterParams params
    ) {
        return geoClusterRepository.findPlannedSlotsInCluster(params);
    }

    public long countActiveOrdersInCluster(
            GeoClusterKey clusterKey,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return geoClusterRepository.countActiveOrdersInCluster(clusterKey, from, to);
    }
}
