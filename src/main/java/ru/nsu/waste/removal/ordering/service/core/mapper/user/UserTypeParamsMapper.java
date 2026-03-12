package ru.nsu.waste.removal.ordering.service.core.mapper.user;

import org.mapstruct.Mapper;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.service.user.param.TieBreakWinnerParams;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface UserTypeParamsMapper {

    TieBreakWinnerParams mapToTieBreakWinnerParams(
            List<RegistrationQuizQuestion> questions,
            Map<Long, Long> answers,
            Map<Integer, RegistrationQuizOption> optionById,
            List<UserType> winners
    );
}
