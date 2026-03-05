package ru.nsu.waste.removal.ordering.service.core.service.registrationquiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuiz;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizOptionRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizQuestionRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationQuizService {

    private static final String REGISTRATION_QUIZ_CODE = "REGISTRATION_HEXAD_LITE";
    private static final String QUIZ_UPDATED_MESSAGE =
            "\u041e\u043f\u0440\u043e\u0441\u043d\u0438\u043a \u043e\u0431\u043d\u043e\u0432\u0438\u043b\u0441\u044f. "
                    + "\u041f\u043e\u0436\u0430\u043b\u0443\u0439\u0441\u0442\u0430, \u0437\u0430\u043f\u043e\u043b\u043d\u0438\u0442\u0435 "
                    + "\u0435\u0433\u043e \u0441\u043d\u043e\u0432\u0430.";
    private static final String QUIZ_WITHOUT_QUESTIONS_MESSAGE =
            "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u043e\u043f\u0440\u043e\u0441\u043d\u0438\u043a \u043d\u0435 "
                    + "\u0441\u043e\u0434\u0435\u0440\u0436\u0438\u0442 \u0432\u043e\u043f\u0440\u043e\u0441\u043e\u0432";
    private static final String QUIZ_WITHOUT_OPTIONS_MESSAGE =
            "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u043e\u043f\u0440\u043e\u0441\u043d\u0438\u043a \u043d\u0435 "
                    + "\u0441\u043e\u0434\u0435\u0440\u0436\u0438\u0442 \u0432\u0430\u0440\u0438\u0430\u043d\u0442\u043e\u0432 \u043e\u0442\u0432\u0435\u0442\u043e\u0432";
    private static final String ANSWER_ALL_QUESTIONS_MESSAGE =
            "\u041e\u0442\u0432\u0435\u0442\u044c\u0442\u0435 \u043d\u0430 \u0432\u0441\u0435 \u0432\u043e\u043f\u0440\u043e\u0441\u044b \u043e\u043f\u0440\u043e\u0441\u0430";
    private static final String ANSWERS_INVALID_MESSAGE =
            "\u041e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u044b \u043d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b\u0435 "
                    + "\u043e\u0442\u0432\u0435\u0442\u044b \u043e\u043f\u0440\u043e\u0441\u0430";
    private static final String QUIZ_NOT_FOUND_MESSAGE =
            "\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0439 \u043e\u043f\u0440\u043e\u0441\u043d\u0438\u043a "
                    + "\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u0438 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d";

    private final RegistrationQuizRepository registrationQuizRepository;
    private final RegistrationQuizQuestionRepository registrationQuizQuestionRepository;
    private final RegistrationQuizOptionRepository registrationQuizOptionRepository;

    public ActiveRegistrationQuizData getActiveQuizData(Long quizIdFromForm) {
        RegistrationQuiz quiz = getActiveQuiz();

        if (quizIdFromForm != null && quizIdFromForm != quiz.id()) {
            throw new QuizValidationException(QUIZ_UPDATED_MESSAGE);
        }

        List<RegistrationQuizQuestion> questions = registrationQuizQuestionRepository.findActiveByQuizId(quiz.id());
        if (questions.isEmpty()) {
            throw new IllegalStateException(QUIZ_WITHOUT_QUESTIONS_MESSAGE);
        }

        List<RegistrationQuizOption> options = registrationQuizOptionRepository.findActiveByQuestionIds(
                questions.stream().map(RegistrationQuizQuestion::id).toList()
        );
        if (options.isEmpty()) {
            throw new IllegalStateException(QUIZ_WITHOUT_OPTIONS_MESSAGE);
        }

        Map<Integer, RegistrationQuizOption> optionById = options.stream()
                .collect(Collectors.toMap(RegistrationQuizOption::id, option -> option));

        return new ActiveRegistrationQuizData(quiz, questions, options, optionById);
    }

    public void validateAnswers(Map<Long, Long> answers) {
        ActiveRegistrationQuizData quizData = getActiveQuizData(null);
        List<RegistrationQuizQuestion> questions = quizData.questions();
        Map<Integer, RegistrationQuizOption> optionById = quizData.optionById();

        if (answers == null || answers.isEmpty()) {
            throw new QuizValidationException(ANSWER_ALL_QUESTIONS_MESSAGE);
        }

        for (RegistrationQuizQuestion question : questions) {
            Long optionIdRaw = answers.get((long) question.id());
            if (optionIdRaw == null) {
                throw new QuizValidationException(ANSWER_ALL_QUESTIONS_MESSAGE);
            }

            RegistrationQuizOption option = optionById.get(optionIdRaw.intValue());
            if (option == null || option.questionId() != question.id()) {
                throw new QuizValidationException(ANSWERS_INVALID_MESSAGE);
            }
        }
    }

    private RegistrationQuiz getActiveQuiz() {
        return registrationQuizRepository.findLatestActiveByCode(REGISTRATION_QUIZ_CODE)
                .orElseThrow(() -> new IllegalStateException(QUIZ_NOT_FOUND_MESSAGE));
    }
}
