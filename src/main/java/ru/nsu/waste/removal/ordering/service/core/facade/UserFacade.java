package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.view.EcoDashboardViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserGamificationViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserHistoryViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserHomeViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserInfoCardViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserLeaderboardViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistory;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.LeaderboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserLeaderboard;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserLeaderboardEntry;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserLeaderboardRepository;
import ru.nsu.waste.removal.ordering.service.core.service.achievement.AchievementService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.EcoDashboardService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.UserHistoryService;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.infocard.InfoCardService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserLeaderboardService;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int WEEK_DAYS = 7;
    private static final int LEADERBOARD_TOP_LIMIT = 10;

    private final UserInfoService userInfoService;
    private final OrderInfoService orderInfoService;
    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserLeaderboardRepository userLeaderboardRepository;
    private final UserLeaderboardService userLeaderboardService;
    private final AchievementService achievementService;
    private final EcoTaskService ecoTaskService;
    private final InfoCardService infoCardService;
    private final EcoDashboardService ecoDashboardService;
    private final UserHistoryService userHistoryService;
    private final Clock clock;

    public UserHomeViewModel getHome(long userId) {
        UserProfileInfo profile = userInfoService.getProfileByUserId(userId);
        ZoneId userZoneId = resolveUserZoneId(userId);

        List<UserHomeViewModel.ActiveOrderViewModel> activeOrders = orderInfoService.findActiveOrders(userId).stream()
                .map(order -> toViewModel(order, userZoneId))
                .toList();

        return new UserHomeViewModel(
                userId,
                profile.totalPoints(),
                profile.currentPoints(),
                profile.userType().name(),
                buildMotivation(userId, profile),
                activeOrders
        );
    }

    public EcoDashboardViewModel getDashboard(long userId, EcoDashboardPeriod period) {
        EcoDashboard dashboard = ecoDashboardService.getDashboard(userId, period);

        return new EcoDashboardViewModel(
                dashboard.userId(),
                dashboard.userType(),
                dashboard.currentPoints(),
                dashboard.totalPoints(),
                new EcoDashboardViewModel.PeriodViewModel(
                        dashboard.period().key(),
                        dashboard.period().title(),
                        dashboard.period().options().stream()
                                .map(option -> new EcoDashboardViewModel.PeriodOptionViewModel(
                                        option.key(),
                                        option.title(),
                                        option.selected()
                                ))
                                .toList()
                ),
                new EcoDashboardViewModel.OrdersStatsViewModel(
                        dashboard.orders().doneTotal(),
                        dashboard.orders().doneSeparate(),
                        dashboard.orders().separateSharePercent()
                ),
                dashboard.fractions(),
                dashboard.insights(),
                dashboard.achiever() == null
                        ? null
                        : new EcoDashboardViewModel.AchieverProgressViewModel(
                                dashboard.achiever().levelId(),
                                dashboard.achiever().nextRequiredPoints(),
                                dashboard.achiever().remaining(),
                                dashboard.achiever().progressPercent(),
                                dashboard.achiever().maxLevelReached()
                )
        );
    }

    public UserHistoryViewModel getHistory(long userId) {
        UserHistory history = userHistoryService.getUserHistory(userId);

        return new UserHistoryViewModel(
                history.userId(),
                history.currentPoints(),
                history.items().stream()
                        .map(item -> new UserHistoryViewModel.ItemViewModel(
                                item.occurredAt(),
                                item.description(),
                                item.pointsDelta(),
                                item.balanceAfter()
                        ))
                        .toList()
        );
    }

    public UserLeaderboardViewModel getLeaderboard(long userId, LeaderboardPeriod period) {
        UserLeaderboard leaderboard = userLeaderboardService.getLeaderboard(userId, period, LEADERBOARD_TOP_LIMIT);
        List<UserLeaderboardViewModel.EntryViewModel> entries = leaderboard.topEntries().stream()
                .map(entry -> toLeaderboardEntryViewModel(entry, userId))
                .toList();

        boolean currentUserInTop = entries.stream().anyMatch(UserLeaderboardViewModel.EntryViewModel::currentUser);
        UserLeaderboardViewModel.EntryViewModel currentUserOutsideTop = null;
        if (!currentUserInTop && leaderboard.currentUserEntry() != null) {
            currentUserOutsideTop = toLeaderboardEntryViewModel(leaderboard.currentUserEntry(), userId);
        }

        List<UserLeaderboardViewModel.PeriodOptionViewModel> periodOptions = List.of(
                toPeriodOptionViewModel(LeaderboardPeriod.WEEK, leaderboard.period()),
                toPeriodOptionViewModel(LeaderboardPeriod.MONTH, leaderboard.period())
        );

        return new UserLeaderboardViewModel(
                leaderboard.userId(),
                leaderboard.period().name(),
                leaderboard.period().title(),
                periodOptions,
                entries,
                currentUserOutsideTop
        );
    }

    public UserInfoCardViewModel getInfoCard(long userId, long cardId) {
        var card = infoCardService.openCard(userId, cardId);
        return new UserInfoCardViewModel(
                userId,
                card.id(),
                card.title(),
                card.description()
        );
    }

    public UserGamificationViewModel getGamification(long userId) {
        UserProfileInfo profile = userInfoService.getProfileByUserId(userId);
        ZoneId userZoneId = resolveUserZoneId(userId);
        Set<Integer> unlockedAchievementIds = achievementService.findUnlockedAchievementIdsByUserId(userId);

        List<UserGamificationViewModel.EcoTaskViewModel> ecoTasks = ecoTaskService.findAllAssignmentsByUserId(userId).stream()
                .map(task -> new UserGamificationViewModel.EcoTaskViewModel(
                        task.userEcoTaskId(),
                        task.ecoTaskId(),
                        task.title(),
                        task.description(),
                        task.points(),
                        localizeEcoTaskStatus(task.status().name()),
                        convertToUserTimezone(task.expiredAt(), userZoneId),
                        convertToUserTimezone(task.completedAt(), userZoneId)
                ))
                .toList();

        List<UserGamificationViewModel.AchievementViewModel> achievements =
                achievementService.findByUserType(profile.userType()).stream()
                        .map(achievement -> new UserGamificationViewModel.AchievementViewModel(
                                achievement.id(),
                                achievement.title(),
                                achievement.description(),
                                unlockedAchievementIds.contains(achievement.id())
                        ))
                        .toList();

        return new UserGamificationViewModel(
                userId,
                ecoTasks,
                achievements
        );
    }

    private UserHomeViewModel.MotivationViewModel buildMotivation(long userId, UserProfileInfo profile) {
        UserType userType = profile.userType();

        if (userType == UserType.ACHIEVER) {
            return new UserHomeViewModel.MotivationViewModel(
                    buildAchieverMotivation(userId, profile.totalPoints()),
                    null,
                    null
            );
        }

        if (userType == UserType.SOCIALIZER) {
            return new UserHomeViewModel.MotivationViewModel(
                    null,
                    buildSocializerMotivation(userId),
                    null
            );
        }

        return new UserHomeViewModel.MotivationViewModel(
                null,
                null,
                buildExplorerMotivation()
        );
    }

    private UserHomeViewModel.AchieverMotivationViewModel buildAchieverMotivation(long userId, long totalPoints) {
        int levelId = achieverProfileRepository.findCurrentLevelId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Профиль достигателя не найден для пользователя с id = %s".formatted(userId)
                ));

        Optional<Level> nextLevel = levelRepository.findNextTargetLevelByTotalPoints(totalPoints);
        if (nextLevel.isEmpty()) {
            return new UserHomeViewModel.AchieverMotivationViewModel(
                    levelId,
                    null,
                    0,
                    FULL_PROGRESS_PERCENT,
                    true
            );
        }

        long required = nextLevel.get().requiredTotalPoints();
        long remaining = Math.max(0L, required - totalPoints);

        return new UserHomeViewModel.AchieverMotivationViewModel(
                levelId,
                required,
                remaining,
                calculateProgressPercent(totalPoints, required),
                false
        );
    }

    private UserHomeViewModel.SocializerMotivationViewModel buildSocializerMotivation(long userId) {
        OffsetDateTime since = OffsetDateTime.now(clock).minusDays(WEEK_DAYS);
        Integer rankPosition = userLeaderboardRepository.findWeeklyRankPosition(userId, since)
                .orElse(null);

        return new UserHomeViewModel.SocializerMotivationViewModel(rankPosition);
    }

    private UserHomeViewModel.ExplorerMotivationViewModel buildExplorerMotivation() {
        List<UserHomeViewModel.InfoCardViewModel> cards = infoCardService.findByUserType(UserType.EXPLORER).stream()
                .map(card -> new UserHomeViewModel.InfoCardViewModel(
                        card.id(),
                        card.title(),
                        card.description()
                ))
                .toList();

        return new UserHomeViewModel.ExplorerMotivationViewModel(cards);
    }

    private UserHomeViewModel.ActiveOrderViewModel toViewModel(ActiveOrderInfo order, ZoneId userZoneId) {
        return new UserHomeViewModel.ActiveOrderViewModel(
                order.orderId(),
                order.type(),
                localizeOrderStatus(order.status()),
                convertToUserTimezone(order.pickupFrom(), userZoneId),
                convertToUserTimezone(order.pickupTo(), userZoneId),
                order.costPoints(),
                localizePaymentStatus(order.paymentStatus()),
                order.fractions()
        );
    }

    private UserLeaderboardViewModel.EntryViewModel toLeaderboardEntryViewModel(
            UserLeaderboardEntry entry,
            long currentUserId
    ) {
        String fullName = entry.fullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = "Пользователь #" + entry.userId();
        }

        return new UserLeaderboardViewModel.EntryViewModel(
                entry.userId(),
                fullName,
                entry.rankPosition(),
                entry.score(),
                entry.userId() == currentUserId
        );
    }

    private UserLeaderboardViewModel.PeriodOptionViewModel toPeriodOptionViewModel(
            LeaderboardPeriod option,
            LeaderboardPeriod selected
    ) {
        return new UserLeaderboardViewModel.PeriodOptionViewModel(
                option.name(),
                option.title(),
                option == selected
        );
    }

    private ZoneId resolveUserZoneId(long userId) {
        String timezone = userInfoService.getGreenSlotContextByUserId(userId).timezone();
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }

    private OffsetDateTime convertToUserTimezone(OffsetDateTime value, ZoneId userZoneId) {
        if (value == null) {
            return null;
        }
        return value.atZoneSameInstant(userZoneId).toOffsetDateTime();
    }

    private String localizeOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Неизвестно";
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "NEW" -> "Новый";
            case "ASSIGNED" -> "Назначен";
            case "DONE" -> "Выполнен";
            case "CANCELLED" -> "Отменен";
            default -> "Неизвестно";
        };
    }

    private String localizePaymentStatus(String paymentStatus) {
        if (paymentStatus == null || paymentStatus.isBlank()) {
            return "Без внутренней оплаты";
        }
        return switch (paymentStatus.trim().toUpperCase(Locale.ROOT)) {
            case "PAID_WITH_POINTS" -> "Оплачен баллами";
            case "UNPAID" -> "Без внутренней оплаты";
            default -> "Без внутренней оплаты";
        };
    }

    private String localizeEcoTaskStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Неизвестно";
        }

        return switch (status.trim().toUpperCase(Locale.ROOT)) {
            case "ASSIGNED" -> "Активно";
            case "DONE" -> "Выполнено";
            case "EXPIRED" -> "Истекло";
            case "CANCELLED" -> "Отменено";
            default -> "Неизвестно";
        };
    }

    private int calculateProgressPercent(long totalPoints, long requiredPoints) {
        if (requiredPoints <= 0L) {
            return FULL_PROGRESS_PERCENT;
        }

        long safeTotalPoints = Math.max(0L, totalPoints);
        long rawPercent = (safeTotalPoints * FULL_PROGRESS_PERCENT) / requiredPoints;

        return (int) Math.max(0L, Math.min(FULL_PROGRESS_PERCENT, rawPercent));
    }
}
