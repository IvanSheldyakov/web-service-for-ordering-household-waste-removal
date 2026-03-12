package ru.nsu.waste.removal.ordering.service.core.mapper.level;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.service.level.param.EmitMaxLevelReachedEventParams;
import ru.nsu.waste.removal.ordering.service.core.service.level.param.LevelUpContentParams;

@Mapper(componentModel = "spring")
public interface LevelParamsMapper {

    EmitMaxLevelReachedEventParams mapToEmitMaxLevelReachedEventParams(
            long userId,
            Level current,
            long oldTotalPoints,
            long newTotalPoints
    );

    LevelUpContentParams mapToLevelUpContentParams(
            Level from,
            Level to,
            long oldTotalPoints,
            long newTotalPoints,
            boolean maxReached
    );
}
