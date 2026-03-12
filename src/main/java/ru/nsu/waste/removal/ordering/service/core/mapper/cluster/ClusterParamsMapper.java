package ru.nsu.waste.removal.ordering.service.core.mapper.cluster;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.repository.cluster.param.FindPlannedSlotsInClusterParams;
import ru.nsu.waste.removal.ordering.service.core.repository.order.param.FindPlannedSlotsInPeriodParams;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface ClusterParamsMapper {

    FindPlannedSlotsInClusterParams mapToFindPlannedSlotsInClusterParams(
            long excludedUserId,
            GeoClusterKey clusterKey,
            OffsetDateTime from,
            OffsetDateTime to
    );

    @Mapping(target = "excludedUserId", source = "userId")
    @Mapping(target = "clusterKey", source = "postalCode")
    FindPlannedSlotsInClusterParams mapToFindPlannedSlotsInClusterParams(FindPlannedSlotsInPeriodParams params);

    default GeoClusterKey map(String postalCode) {
        return new GeoClusterKey(postalCode);
    }
}
