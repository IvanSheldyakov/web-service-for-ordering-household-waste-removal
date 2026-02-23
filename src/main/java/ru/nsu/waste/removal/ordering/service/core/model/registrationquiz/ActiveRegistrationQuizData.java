package ru.nsu.waste.removal.ordering.service.core.model.registrationquiz;

import java.util.List;
import java.util.Map;

public record ActiveRegistrationQuizData(
        RegistrationQuiz quiz,
        List<RegistrationQuizQuestion> questions,
        List<RegistrationQuizOption> options,
        Map<Integer, RegistrationQuizOption> optionById
) {
}
