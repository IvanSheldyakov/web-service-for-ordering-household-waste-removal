package ru.nsu.waste.removal.ordering.service.core.service.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.AchievementCode;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.AchievementRule;
import ru.nsu.waste.removal.ordering.service.core.model.event.AchievementUnlockedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.achievement.AchievementRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.achievement.AchievementUserRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserLeaderboardRepository;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final int TOP_10_THRESHOLD = 10;
    private static final int TOP_3_THRESHOLD = 3;
    private static final int EXPLORED_FRACTIONS_3_THRESHOLD = 3;
    private static final int EXPLORED_FRACTIONS_ALL_THRESHOLD = 5;
    private static final int GREEN_ORDERS_5_THRESHOLD = 5;
    private static final int INFO_CARD_VIEWS_5_THRESHOLD = 5;
    private static final int SEPARATE_ORDERS_5_THRESHOLD = 5;

    private final AchievementRepository achievementRepository;
    private final AchievementUserRepository achievementUserRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserInfoRepository userInfoRepository;
    private final UserLeaderboardRepository userLeaderboardRepository;
    private final OrderInfoService orderInfoService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public List<Achievement> findByUserType(UserType userType) {
        return achievementRepository.findByUserType(userType);
    }

    public void processUserAction(long userId, UserActionEventType eventType) {
        if (eventType == UserActionEventType.ACHIEVEMENT_UNLOCKED) {
            return;
        }

        UserType userType = userInfoRepository.findUserTypeByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User with id = %s is not found".formatted(userId)
                ));

        List<AchievementRule> candidates =
                achievementRepository.findTriggeredByUserTypeAndEvent(userType, eventType.dbName());
        for (AchievementRule candidate : candidates) {
            if (!isConditionMet(userId, candidate.code())) {
                continue;
            }

            if (achievementUserRepository.unlockForUser(userId, candidate.id())) {
                userActionHistoryRepository.addEvent(
                        userId,
                        UserActionEventType.ACHIEVEMENT_UNLOCKED.dbName(),
                        buildUnlockedContentJson(candidate),
                        0
                );
            }
        }
    }

    private boolean isConditionMet(long userId, AchievementCode achievementCode) {
        return switch (achievementCode) {
            case ACH_FIRST_ORDER -> orderInfoService.countDoneOrders(userId) >= 1;
            case ACH_FIRST_SEPARATE -> orderInfoService.countDoneSeparateOrders(userId) >= 1;
            case ACH_SEPARATE_5 -> orderInfoService.countDoneSeparateOrders(userId) >= SEPARATE_ORDERS_5_THRESHOLD;
            case ACH_LEVEL_UP ->
                    userActionHistoryRepository.countByUserIdAndEventType(userId, UserActionEventType.LEVEL_UP.dbName()) >= 1;
            case SOC_OPEN_LEADERBOARD ->
                    userActionHistoryRepository.countByUserIdAndEventType(userId, UserActionEventType.LEADERBOARD_OPENED.dbName()) >= 1;
            case SOC_TOP10_WEEK -> weeklyRankAtMost(userId, TOP_10_THRESHOLD);
            case SOC_TOP3_WEEK -> weeklyRankAtMost(userId, TOP_3_THRESHOLD);
            case SOC_GREEN_HELPER_5 -> orderInfoService.countDoneGreenOrders(userId) >= GREEN_ORDERS_5_THRESHOLD;
            case EXP_OPEN_PROFILE ->
                    userActionHistoryRepository.countByUserIdAndEventType(userId, UserActionEventType.ECO_PROFILE_OPENED.dbName()) >= 1;
            case EXP_CARDS_5 ->
                    userActionHistoryRepository.countByUserIdAndEventType(userId, UserActionEventType.INFO_CARD_VIEWED.dbName()) >= INFO_CARD_VIEWS_5_THRESHOLD;
            case EXP_NEW_FRACTIONS_3 ->
                    orderInfoService.countDistinctFractionsInDoneSeparateOrders(userId) >= EXPLORED_FRACTIONS_3_THRESHOLD;
            case EXP_NEW_FRACTIONS_ALL_5 ->
                    orderInfoService.countDistinctFractionsInDoneSeparateOrders(userId) >= EXPLORED_FRACTIONS_ALL_THRESHOLD;
        };
    }

    private boolean weeklyRankAtMost(long userId, int threshold) {
        OffsetDateTime since = OffsetDateTime.now(clock).minusDays(7);
        return userLeaderboardRepository.findWeeklyRankPosition(userId, since)
                .map(rank -> rank <= threshold)
                .orElse(false);
    }

    private String buildUnlockedContentJson(AchievementRule achievement) {
        AchievementUnlockedEventContent content = new AchievementUnlockedEventContent(
                achievement.id(),
                achievement.code().dbCode(),
                achievement.title()
        );

        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize achievement unlock content", exception);
        }
    }
}
