package ru.nsu.waste.removal.ordering.service.core.service.registrationquiz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.view.RegistrationQuizViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuiz;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizOptionRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizQuestionRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.registrationquiz.RegistrationQuizRepository;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegistrationQuizService {

    private static final String REGISTRATION_QUIZ_CODE = "REGISTRATION_HEXAD_LITE";

    private final RegistrationQuizRepository registrationQuizRepository;
    private final RegistrationQuizQuestionRepository registrationQuizQuestionRepository;
    private final RegistrationQuizOptionRepository registrationQuizOptionRepository;

    public ActiveRegistrationQuizData getActiveQuizData(Long quizIdFromForm) {
        RegistrationQuiz quiz = getActiveQuiz();

        if (quizIdFromForm != null && quizIdFromForm != quiz.id()) {
            throw new QuizValidationException("Опросник обновился. Пожалуйста, заполните его снова.");
        }

        List<RegistrationQuizQuestion> questions = registrationQuizQuestionRepository.findActiveByQuizId(quiz.id());
        if (questions.isEmpty()) {
            throw new IllegalStateException("Активный опросник не содержит вопросов");
        }

        List<RegistrationQuizOption> options = registrationQuizOptionRepository.findActiveByQuestionIds(
                questions.stream().map(RegistrationQuizQuestion::id).toList()
        );
        if (options.isEmpty()) {
            throw new IllegalStateException("Активный опросник не содержит вариантов ответов");
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
            throw new QuizValidationException("Ответьте на все вопросы опроса");
        }
        for (RegistrationQuizQuestion question : questions) {
            Long optionIdRaw = answers.get((long) question.id());
            if (optionIdRaw == null) {
                throw new QuizValidationException("Ответьте на все вопросы опроса");
            }
            RegistrationQuizOption option = optionById.get(optionIdRaw.intValue());
            if (option == null || option.questionId() != question.id()) {
                throw new QuizValidationException("Обнаружены некорректные ответы опроса");
            }
        }
    }

    public RegistrationQuizViewModel getActiveQuizView() {
        ActiveRegistrationQuizData quizData = getActiveQuizData(null);
        Map<Integer, List<RegistrationQuizOption>> optionsByQuestion = quizData.options().stream()
                .collect(Collectors.groupingBy(RegistrationQuizOption::questionId));

        List<RegistrationQuizViewModel.QuestionViewModel> questionViews = quizData.questions().stream()
                .map(question -> getQuestionViewModel(question, optionsByQuestion))
                .toList();

        return new RegistrationQuizViewModel(
                quizData.quiz().id(),
                quizData.quiz().code(),
                quizData.quiz().version(),
                questionViews
        );
    }

    private RegistrationQuiz getActiveQuiz() {
        return registrationQuizRepository.findLatestActiveByCode(REGISTRATION_QUIZ_CODE)
                .orElseThrow(() -> new IllegalStateException("Активный опросник регистрации не найден"));
    }

    private static RegistrationQuizViewModel.QuestionViewModel getQuestionViewModel(
            RegistrationQuizQuestion question,
            Map<Integer, List<RegistrationQuizOption>> optionsByQuestion
    ) {
        return new RegistrationQuizViewModel.QuestionViewModel(
                question.id(),
                question.ord(),
                question.text(),
                question.tiebreak(),
                optionsByQuestion.getOrDefault(question.id(), List.of()).stream()
                        .map(option -> new RegistrationQuizViewModel.OptionViewModel(
                                option.id(),
                                option.ord(),
                                option.text()
                        ))
                        .toList()
        );
    }
}
