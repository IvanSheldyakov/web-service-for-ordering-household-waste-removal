package ru.nsu.waste.removal.ordering.service.core.service.user.param;

import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;

import java.util.List;
import java.util.Map;

public record TieBreakWinnerParams(
        List<RegistrationQuizQuestion> questions,
        Map<Long, Long> answers,
        Map<Integer, RegistrationQuizOption> optionById,
        List<UserType> winners
) {
}
