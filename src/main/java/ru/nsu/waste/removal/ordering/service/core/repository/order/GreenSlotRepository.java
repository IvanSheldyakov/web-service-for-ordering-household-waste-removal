package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.repository.cluster.GeoClusterRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GreenSlotRepository {

    private final GeoClusterRepository geoClusterRepository;

    public List<GreenSlot> findPlannedSlotsInPeriod(
            long userId,
            String postalCode,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return geoClusterRepository.findPlannedSlotsInCluster(
                userId,
                new GeoClusterKey(postalCode),
                from,
                to
        );
    }
}
