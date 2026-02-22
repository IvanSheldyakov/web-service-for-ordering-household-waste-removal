package ru.nsu.waste.removal.ordering.service.core.model.registrationquiz;

public record RegistrationQuizQuestion(
        int id,
        int quizId,
        String code,
        int ord,
        String text,
        boolean tiebreak
) {
}
