package ru.nsu.waste.removal.ordering.service.core.mapper.user;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.repository.user.param.AddUserInfoParams;
import ru.nsu.waste.removal.ordering.service.core.repository.user.param.UpdateRewardStateParams;

@Mapper(componentModel = "spring")
public interface UserInfoParamsMapper {

    default AddUserInfoParams mapToAddUserInfoParams(
            int typeId,
            long addressId,
            long personId,
            long initialPoints
    ) {
        return new AddUserInfoParams(typeId, addressId, personId, initialPoints);
    }

    default UpdateRewardStateParams mapToUpdateRewardStateParams(
            long userId,
            long totalPoints,
            long currentPoints,
            long habitStrength
    ) {
        return new UpdateRewardStateParams(userId, totalPoints, currentPoints, habitStrength);
    }
}
