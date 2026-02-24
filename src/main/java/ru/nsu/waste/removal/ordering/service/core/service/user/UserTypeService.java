package ru.nsu.waste.removal.ordering.service.core.service.user;

import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UserTypeService {

    private static final List<UserType> TYPE_PRIORITY = List.of(
            UserType.ACHIEVER,
            UserType.SOCIALIZER,
            UserType.EXPLORER
    );

    public UserType resolveUserType(
            ActiveRegistrationQuizData quizData,
            Map<Long, Long> answers
    ) {
        List<RegistrationQuizQuestion> questions = quizData.questions();
        Map<Integer, RegistrationQuizOption> optionById = quizData.optionById();

        Map<UserType, Integer> scoreByType = new EnumMap<>(UserType.class);
        for (RegistrationQuizQuestion question : questions) {
            long optionId = answers.get((long) question.id());
            RegistrationQuizOption option = optionById.get((int) optionId);
            UserType userType = UserType.fromId(option.userTypeId());
            scoreByType.merge(userType, option.score(), Integer::sum);
        }

        int maxScore = scoreByType.values().stream()
                .max(Integer::compareTo)
                .orElseThrow(() -> new QuizValidationException("Не удалось определить тип пользователя"));

        List<UserType> winners = scoreByType.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();
        if (winners.size() == 1) {
            return winners.getFirst();
        }

        Optional<UserType> tieBreakWinner = resolveTieBreakWinner(questions, answers, optionById, winners);
        return tieBreakWinner.orElseGet(() -> resolveByPriority(winners));
    }

    private Optional<UserType> resolveTieBreakWinner(
            List<RegistrationQuizQuestion> questions,
            Map<Long, Long> answers,
            Map<Integer, RegistrationQuizOption> optionById,
            List<UserType> winners
    ) {
        for (RegistrationQuizQuestion question : questions) {
            if (!question.tiebreak()) {
                continue;
            }
            Long selectedOptionId = answers.get((long) question.id());
            if (selectedOptionId == null) {
                continue;
            }
            RegistrationQuizOption option = optionById.get(selectedOptionId.intValue());
            if (option == null) {
                continue;
            }
            UserType userType = UserType.fromId(option.userTypeId());
            if (winners.contains(userType)) {
                return Optional.of(userType);
            }
        }
        return Optional.empty();
    }

    private UserType resolveByPriority(List<UserType> winners) {
        for (UserType userType : TYPE_PRIORITY) {
            if (winners.contains(userType)) {
                return userType;
            }
        }
        return winners.stream()
                .min((left, right) -> Integer.compare(left.getId(), right.getId()))
                .orElseThrow(() -> new IllegalStateException("Не удалось разрешить ничью по типу пользователя"));
    }
}
