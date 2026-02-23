package ru.nsu.waste.removal.ordering.service.core.service.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserTypeInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.achievement.AchievementRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.infocard.InfoCardRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.person.PersonInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserTypeRepository;
import ru.nsu.waste.removal.ordering.service.core.service.address.AddressService;
import ru.nsu.waste.removal.ordering.service.core.service.address.mapper.AddressMapper;
import ru.nsu.waste.removal.ordering.service.core.model.address.AddressCreationData;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.person.mapper.PersonInfoMapper;
import ru.nsu.waste.removal.ordering.service.core.model.person.PersonCreationData;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserTypeService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final PersonInfoRepository personInfoRepository;
    private final PersonInfoService personInfoService;
    private final PersonInfoMapper personInfoMapper;
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserInfoService userInfoService;
    private final EcoTaskService ecoTaskService;
    private final LevelRepository levelRepository;
    private final UserTypeRepository userTypeRepository;
    private final InfoCardRepository infoCardRepository;
    private final AchievementRepository achievementRepository;
    private final RegistrationQuizService registrationQuizService;
    private final UserTypeService userTypeService;

    public boolean isPhoneRegistered(String phone) {
        return personInfoRepository.existsByPhone(Long.parseLong(phone));
    }

    @Transactional
    public RegistrationResultViewModel register(RegistrationForm form, QuizAnswerForm quizAnswerForm) {
        ZoneId zoneId = validateTimezone(form.getTimezone());

        ActiveRegistrationQuizData quizData = registrationQuizService.getActiveQuizData(quizAnswerForm.getQuizId());

        Map<Long, Long> answers = quizAnswerForm.getAnswers() == null ? Map.of() : quizAnswerForm.getAnswers();
        registrationQuizService.validateAnswers(answers);

        int userTypeId = userTypeService.resolveUserTypeId(quizData, answers);

        PersonCreationData personCreationData = personInfoMapper.toPersonCreationData(form);
        long personId = personInfoService.add(personCreationData);

        AddressCreationData addressCreationData = addressMapper.toAddressCreationData(form, zoneId);
        long addressId = addressService.add(addressCreationData);

        long userId = userInfoService.add(userTypeId, addressId, personId);

        List<AssignedEcoTask> assignedTasks =
                ecoTaskService.assignStarterTasksAndGetAssigned(userTypeId, userId, zoneId);

        List<Achievement> achievements = achievementRepository.findByUserTypeId(userTypeId);
        UserType userType = userTypeRepository.findById(userTypeId)
                .map(UserTypeInfo::userType)
                .orElseThrow(() -> new IllegalStateException("Не найден user_type для id=" + userTypeId));
        String typeName = userType.name();

        List<InfoCard> cards = userType == UserType.EXPLORER ? infoCardRepository.findRandom(3) : List.of();
        RegistrationResultViewModel.MotivationBlockViewModel motivationBlock =
                buildMotivation(userType, form.getPostalCode().trim());

        return new RegistrationResultViewModel(
                userId,
                typeName,
                new RegistrationResultViewModel.BalanceViewModel(0, 0),
                motivationBlock,
                assignedTasks.stream()
                        .map(task -> new RegistrationResultViewModel.EcoTaskViewModel(
                                task.id(),
                                task.title(),
                                task.description(),
                                task.points(),
                                task.expiredAt()
                        ))
                        .toList(),
                achievements.stream()
                        .map(achievement -> new RegistrationResultViewModel.AchievementViewModel(
                                achievement.id(),
                                achievement.title(),
                                achievement.description()
                        ))
                        .toList(),
                cards.stream()
                        .map(card -> new RegistrationResultViewModel.InfoCardViewModel(
                                card.id(),
                                card.title(),
                                card.description()
                        ))
                        .toList()
        );
    }

    private RegistrationResultViewModel.MotivationBlockViewModel buildMotivation(UserType userType, String postalCode) {
        if (userType == UserType.ACHIEVER) {
            Level firstLevel = levelRepository.findLowestLevel();
            return new RegistrationResultViewModel.MotivationBlockViewModel(
                    userType.name(),
                    firstLevel.requiredTotalPoints(),
                    0,
                    null,
                    "Ваш прогресс начат. До первого уровня осталось набрать очки."
            );
        }
        if (userType == UserType.SOCIALIZER) {
            return new RegistrationResultViewModel.MotivationBlockViewModel(
                    userType.name(),
                    null,
                    null,
                    postalCode,
                    "Рейтинг появится после первых действий."
            );
        }
        return new RegistrationResultViewModel.MotivationBlockViewModel(
                userType.name(),
                null,
                null,
                null,
                "Изучайте карточки и находите полезные практики сортировки."
        );
    }

    private ZoneId validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new QuizValidationException("Некорректный часовой пояс");
        }
    }
}
