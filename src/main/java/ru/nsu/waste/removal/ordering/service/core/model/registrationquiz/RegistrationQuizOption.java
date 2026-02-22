package ru.nsu.waste.removal.ordering.service.core.model.registrationquiz;

public record RegistrationQuizOption(
        int id,
        int questionId,
        int ord,
        String text,
        int userTypeId,
        int score
) {
}
