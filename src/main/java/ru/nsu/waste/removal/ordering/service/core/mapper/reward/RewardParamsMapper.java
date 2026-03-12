package ru.nsu.waste.removal.ordering.service.core.mapper.reward;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.service.reward.param.RewardContentParams;

@Mapper(componentModel = "spring")
public interface RewardParamsMapper {

    default RewardContentParams mapToRewardContentParams(
            boolean success,
            double oldStrength,
            double newStrength,
            double fValue,
            long calculatedDelta,
            long appliedDelta
    ) {
        return new RewardContentParams(success, oldStrength, newStrength, fValue, calculatedDelta, appliedDelta);
    }
}
