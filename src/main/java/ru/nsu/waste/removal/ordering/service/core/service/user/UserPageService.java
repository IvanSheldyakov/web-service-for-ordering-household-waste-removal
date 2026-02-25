package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.service.achievement.AchievementService;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.infocard.InfoCardService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserPageService {

    private final UserInfoService userInfoService;
    private final EcoTaskService ecoTaskService;
    private final AchievementService achievementService;
    private final InfoCardService infoCardService;
    private final LevelRepository levelRepository;

    public RegistrationResultViewModel getUserPage(long userId) {
        UserProfileInfo userProfile = userInfoService.getProfileByUserId(userId);
        UserType userType = userProfile.userType();

        List<AssignedEcoTask> assignedTasks = ecoTaskService.findAssignedByUserId(userId);
        List<Achievement> achievements = achievementService.findByUserType(userType);
        List<InfoCard> cards = infoCardService.findByUserType(userType);

        RegistrationResultViewModel.MotivationBlockViewModel motivationBlock =
                buildMotivation(userType, userProfile.postalCode(), userProfile.currentPoints());

        return new RegistrationResultViewModel(
                userId,
                userType.getRussianName(),
                new RegistrationResultViewModel.BalanceViewModel(
                        userProfile.totalPoints(),
                        userProfile.currentPoints()
                ),
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

    private RegistrationResultViewModel.MotivationBlockViewModel buildMotivation(
            UserType userType,
            String postalCode,
            long currentPoints
    ) {
        if (userType == UserType.ACHIEVER) {
            Level firstLevel = levelRepository.findLowestLevel();
            return new RegistrationResultViewModel.MotivationBlockViewModel(
                    userType.getRussianName(),
                    firstLevel.requiredTotalPoints(),
                    calculateProgressPercent(currentPoints, firstLevel.requiredTotalPoints()),
                    null,
                    "Ваш прогресс начат. До первого уровня осталось набрать очки."
            );
        }
        if (userType == UserType.SOCIALIZER) {
            return new RegistrationResultViewModel.MotivationBlockViewModel(
                    userType.getRussianName(),
                    null,
                    null,
                    postalCode,
                    "Рейтинг появится после первых действий."
            );
        }
        return new RegistrationResultViewModel.MotivationBlockViewModel(
                userType.getRussianName(),
                null,
                null,
                null,
                "Изучайте карточки и находите полезные практики сортировки."
        );
    }

    private Integer calculateProgressPercent(long currentPoints, int requiredPoints) {
        if (requiredPoints <= 0) {
            return 0;
        }
        long safeCurrent = Math.max(0, currentPoints);
        return (int) Math.min(100, (safeCurrent * 100) / requiredPoints);
    }
}
