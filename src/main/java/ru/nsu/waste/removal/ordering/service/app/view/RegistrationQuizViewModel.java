package ru.nsu.waste.removal.ordering.service.app.view;

import java.util.List;

public record RegistrationQuizViewModel(
        long quizId,
        String code,
        int version,
        List<QuestionViewModel> questions
) {
    public record QuestionViewModel(
            long id,
            int ord,
            String text,
            boolean tiebreak,
            List<OptionViewModel> options
    ) {
    }

    public record OptionViewModel(
            long id,
            int ord,
            String text
    ) {
    }
}
