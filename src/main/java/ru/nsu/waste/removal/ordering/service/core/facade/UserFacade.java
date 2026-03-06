package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.view.EcoDashboardViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserHistoryViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.UserHomeViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistory;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserLeaderboardRepository;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.EcoDashboardService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.UserHistoryService;
import ru.nsu.waste.removal.ordering.service.core.service.infocard.InfoCardService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserFacade {

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final int WEEK_DAYS = 7;

    private final UserInfoService userInfoService;
    private final OrderInfoService orderInfoService;
    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserLeaderboardRepository userLeaderboardRepository;
    private final InfoCardService infoCardService;
    private final EcoDashboardService ecoDashboardService;
    private final UserHistoryService userHistoryService;
    private final Clock clock;

    public UserHomeViewModel getHome(long userId) {
        UserProfileInfo profile = userInfoService.getProfileByUserId(userId);

        List<UserHomeViewModel.ActiveOrderViewModel> activeOrders = orderInfoService.findActiveOrders(userId).stream()
                .map(this::toViewModel)
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
                        "Achiever profile is not found for user id = %s".formatted(userId)
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

    private UserHomeViewModel.ActiveOrderViewModel toViewModel(ActiveOrderInfo order) {
        return new UserHomeViewModel.ActiveOrderViewModel(
                order.orderId(),
                order.type(),
                order.status(),
                order.pickupFrom(),
                order.pickupTo(),
                order.fractions()
        );
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
