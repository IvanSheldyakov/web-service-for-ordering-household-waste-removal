package ru.nsu.waste.removal.ordering.service.core.repository.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.mapper.cluster.ClusterParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.repository.cluster.GeoClusterRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.order.param.FindPlannedSlotsInPeriodParams;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GreenSlotRepository {

    private final GeoClusterRepository geoClusterRepository;
    private final ClusterParamsMapper clusterParamsMapper;

    public List<GreenSlot> findPlannedSlotsInPeriod(FindPlannedSlotsInPeriodParams params) {
        return geoClusterRepository.findPlannedSlotsInCluster(
                clusterParamsMapper.mapToFindPlannedSlotsInClusterParams(params)
        );
    }
}
