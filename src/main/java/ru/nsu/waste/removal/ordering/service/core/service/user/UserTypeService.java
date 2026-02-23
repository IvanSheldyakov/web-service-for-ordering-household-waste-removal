package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizOption;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.RegistrationQuizQuestion;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserTypeInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserTypeRepository;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserTypeService {

    private static final List<UserType> TYPE_PRIORITY = List.of(
            UserType.ACHIEVER,
            UserType.SOCIALIZER,
            UserType.EXPLORER
    );

    private final UserTypeRepository userTypeRepository;

    public int resolveUserTypeId(
            ActiveRegistrationQuizData quizData,
            Map<Long, Long> answers
    ) {
        List<RegistrationQuizQuestion> questions = quizData.questions();
        Map<Integer, RegistrationQuizOption> optionById = quizData.optionById();

        Map<Integer, Integer> scoreByType = new HashMap<>();
        for (RegistrationQuizQuestion question : questions) {
            long optionId = answers.get((long) question.id());
            RegistrationQuizOption option = optionById.get((int) optionId);
            scoreByType.merge(option.userTypeId(), option.score(), Integer::sum);
        }

        int maxScore = scoreByType.values().stream()
                .max(Integer::compareTo)
                .orElseThrow(() -> new QuizValidationException("Не удалось определить тип пользователя"));

        List<Integer> winners = scoreByType.entrySet().stream()
                .filter(entry -> entry.getValue() == maxScore)
                .map(Map.Entry::getKey)
                .toList();
        if (winners.size() == 1) {
            return winners.getFirst();
        }

        Optional<Integer> tieBreakWinner = resolveTieBreakWinner(questions, answers, optionById, winners);
        return tieBreakWinner.orElseGet(() -> resolveByPriority(winners));
    }

    private Optional<Integer> resolveTieBreakWinner(
            List<RegistrationQuizQuestion> questions,
            Map<Long, Long> answers,
            Map<Integer, RegistrationQuizOption> optionById,
            List<Integer> winners
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
            if (option != null && winners.contains(option.userTypeId())) {
                return Optional.of(option.userTypeId());
            }
        }
        return Optional.empty();
    }

    private int resolveByPriority(List<Integer> winners) {
        Map<UserType, Integer> typeIdsByName = userTypeRepository.findAll().stream()
                .collect(Collectors.toMap(UserTypeInfo::userType, UserTypeInfo::id));
        for (UserType userType : TYPE_PRIORITY) {
            Integer typeId = typeIdsByName.get(userType);
            if (typeId != null && winners.contains(typeId)) {
                return typeId;
            }
        }
        return winners.stream()
                .min(Integer::compareTo)
                .orElseThrow(() -> new IllegalStateException("Не удалось разрешить ничью по типу пользователя"));
    }
}
