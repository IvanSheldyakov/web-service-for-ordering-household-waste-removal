package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.service.achievement.AchievementService;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.infocard.InfoCardService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String ACHIEVER_MOTIVATION_MESSAGE =
            "\u0412\u0430\u0448 \u043f\u0440\u043e\u0433\u0440\u0435\u0441\u0441 \u043d\u0430\u0447\u0430\u0442. "
                    + "\u0414\u043e \u043f\u0435\u0440\u0432\u043e\u0433\u043e \u0443\u0440\u043e\u0432\u043d\u044f "
                    + "\u043e\u0441\u0442\u0430\u043b\u043e\u0441\u044c \u043d\u0430\u0431\u0440\u0430\u0442\u044c \u043e\u0447\u043a\u0438.";
    private static final String SOCIALIZER_MOTIVATION_MESSAGE =
            "\u0420\u0435\u0439\u0442\u0438\u043d\u0433 \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f "
                    + "\u043f\u043e\u0441\u043b\u0435 \u043f\u0435\u0440\u0432\u044b\u0445 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0439.";
    private static final String EXPLORER_MOTIVATION_MESSAGE =
            "\u0418\u0437\u0443\u0447\u0430\u0439\u0442\u0435 \u043a\u0430\u0440\u0442\u043e\u0447\u043a\u0438 "
                    + "\u0438 \u043d\u0430\u0445\u043e\u0434\u0438\u0442\u0435 \u043f\u043e\u043b\u0435\u0437\u043d\u044b\u0435 "
                    + "\u043f\u0440\u0430\u043a\u0442\u0438\u043a\u0438 \u0441\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0438.";

    private final UserInfoService userInfoService;
    private final EcoTaskService ecoTaskService;
    private final AchievementService achievementService;
    private final InfoCardService infoCardService;
    private final LevelRepository levelRepository;

    public UserRegistrationResult getUserProfile(long userId) {
        UserProfileInfo userProfile = userInfoService.getProfileByUserId(userId);
        UserType userType = userProfile.userType();

        List<AssignedEcoTask> assignedTasks = ecoTaskService.findAssignedByUserId(userId);
        List<Achievement> achievements = achievementService.findByUserType(userType);
        List<InfoCard> cards = infoCardService.findByUserType(userType);

        UserRegistrationResult.MotivationBlock motivationBlock =
                buildMotivation(userType, userProfile.postalCode(), userProfile.currentPoints());

        return new UserRegistrationResult(
                userId,
                userType.getRussianName(),
                new UserRegistrationResult.Balance(
                        userProfile.totalPoints(),
                        userProfile.currentPoints()
                ),
                motivationBlock,
                assignedTasks.stream()
                        .map(task -> new UserRegistrationResult.EcoTask(
                                task.id(),
                                task.title(),
                                task.description(),
                                task.points(),
                                task.expiredAt()
                        ))
                        .toList(),
                achievements.stream()
                        .map(achievement -> new UserRegistrationResult.Achievement(
                                achievement.id(),
                                achievement.title(),
                                achievement.description()
                        ))
                        .toList(),
                cards.stream()
                        .map(card -> new UserRegistrationResult.InfoCard(
                                card.id(),
                                card.title(),
                                card.description()
                        ))
                        .toList()
        );
    }

    private UserRegistrationResult.MotivationBlock buildMotivation(
            UserType userType,
            String postalCode,
            long currentPoints
    ) {
        if (userType == UserType.ACHIEVER) {
            Level firstLevel = levelRepository.findLowestLevel();
            return new UserRegistrationResult.MotivationBlock(
                    userType.getRussianName(),
                    firstLevel.requiredTotalPoints(),
                    calculateProgressPercent(currentPoints, firstLevel.requiredTotalPoints()),
                    null,
                    ACHIEVER_MOTIVATION_MESSAGE
            );
        }
        if (userType == UserType.SOCIALIZER) {
            return new UserRegistrationResult.MotivationBlock(
                    userType.getRussianName(),
                    null,
                    null,
                    postalCode,
                    SOCIALIZER_MOTIVATION_MESSAGE
            );
        }
        return new UserRegistrationResult.MotivationBlock(
                userType.getRussianName(),
                null,
                null,
                null,
                EXPLORER_MOTIVATION_MESSAGE
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
