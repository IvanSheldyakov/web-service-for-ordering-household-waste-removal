package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.mapper.ecoprofile.EcoDashboardParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param.BuildDoneFiltersParams;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.param.BuildInsightsParams;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EcoDashboardService {

    private static final int FULL_PROGRESS_PERCENT = 100;
    private static final long ZERO_POINTS_DIFFERENCE = 0L;
    private static final OffsetDateTime ALL_PERIOD_START = OffsetDateTime.parse("1970-01-01T00:00:00Z");

    private static final String NO_SEPARATE_ORDERS_MESSAGE =
            "\u041f\u043e\u043a\u0430 \u043d\u0435\u0442 \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 "
                    + "\u0437\u0430\u043a\u0430\u0437\u043e\u0432 \u0437\u0430 \u043f\u0435\u0440\u0438\u043e\u0434 \u2014 "
                    + "\u043d\u0430\u0447\u043d\u0438\u0442\u0435 \u0441 \u043f\u0435\u0440\u0432\u043e\u0439 \u0444\u0440\u0430\u043a\u0446\u0438\u0438.";
    private static final String ALL_TIME_SEPARATE_ORDERS_PREFIX =
            "\u0417\u0430 \u0432\u0441\u0451 \u0432\u0440\u0435\u043c\u044f \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 "
                    + "\u0437\u0430\u043a\u0430\u0437\u043e\u0432: ";
    private static final String PERIOD_SEPARATE_ORDERS_PREFIX =
            "\u0417\u0430 \u043f\u0435\u0440\u0438\u043e\u0434 \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 "
                    + "\u0437\u0430\u043a\u0430\u0437\u043e\u0432: ";
    private static final String SEPARATE_ORDERS_SHARE_PREFIX =
            "\u0414\u043e\u043b\u044f \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 \u0437\u0430\u043a\u0430\u0437\u043e\u0432: ";
    private static final String PERIOD_MORE_SUFFIX =
            " \u0431\u043e\u043b\u044c\u0448\u0435, \u0447\u0435\u043c \u0437\u0430 \u043f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439 \u043f\u0435\u0440\u0438\u043e\u0434.";
    private static final String PERIOD_LESS_SUFFIX =
            " \u043c\u0435\u043d\u044c\u0448\u0435, \u0447\u0435\u043c \u0437\u0430 \u043f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439 \u043f\u0435\u0440\u0438\u043e\u0434.";
    private static final String PERIOD_EQUAL_SUFFIX =
            ". \u042d\u0442\u043e \u0441\u0442\u043e\u043b\u044c\u043a\u043e \u0436\u0435, \u0441\u043a\u043e\u043b\u044c\u043a\u043e "
                    + "\u0437\u0430 \u043f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0439 \u043f\u0435\u0440\u0438\u043e\u0434.";
    private static final String SHARE_GROWTH_PREFIX =
            "\u0414\u043e\u043b\u044f \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 \u0437\u0430\u043a\u0430\u0437\u043e\u0432 "
                    + "\u0432\u044b\u0440\u043e\u0441\u043b\u0430 \u043d\u0430 ";
    private static final String SHARE_DECLINE_PREFIX =
            "\u0414\u043e\u043b\u044f \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 \u0437\u0430\u043a\u0430\u0437\u043e\u0432 "
                    + "\u0441\u043d\u0438\u0437\u0438\u043b\u0430\u0441\u044c \u043d\u0430 ";
    private static final String SHARE_UNCHANGED_PREFIX =
            "\u0414\u043e\u043b\u044f \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 \u0437\u0430\u043a\u0430\u0437\u043e\u0432 "
                    + "\u043d\u0435 \u0438\u0437\u043c\u0435\u043d\u0438\u043b\u0430\u0441\u044c: ";
    private static final String PP_SUFFIX = " \u043f.\u043f.: ";
    private static final String AGAINST_SUFFIX = "% \u043f\u0440\u043e\u0442\u0438\u0432 ";

    private final UserInfoService userInfoService;
    private final OrderInfoService orderInfoService;
    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final EcoDashboardParamsMapper ecoDashboardParamsMapper;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EcoDashboard getDashboard(long userId, EcoDashboardPeriod period) {
        UserProfileInfo profile = userInfoService.getProfileByUserId(userId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        EcoDashboard.OrdersStats currentStats = buildCurrentStats(userId, period, now);
        List<String> fractions = buildFractions(userId, period, now);
        List<String> insights = buildInsights(
                ecoDashboardParamsMapper.mapToBuildInsightsParams(userId, period, now, currentStats)
        );
        EcoDashboard.AchieverProgress achiever =
                buildAchieverProgress(userId, profile.userType(), profile.totalPoints());

        addEcoProfileOpenedEvent(userId, period);

        return new EcoDashboard(
                userId,
                profile.userType().name(),
                profile.currentPoints(),
                profile.totalPoints(),
                buildPeriod(period),
                currentStats,
                fractions,
                insights,
                achiever
        );
    }

    private EcoDashboard.OrdersStats buildCurrentStats(
            long userId,
            EcoDashboardPeriod period,
            OffsetDateTime now
    ) {
        if (period == EcoDashboardPeriod.ALL) {
            long doneTotal = orderInfoService.countDoneOrders(userId);
            long doneSeparate = orderInfoService.countDoneSeparateOrders(userId);

            return new EcoDashboard.OrdersStats(
                    doneTotal,
                    doneSeparate,
                    calculateSharePercent(doneSeparate, doneTotal)
            );
        }

        OffsetDateTime from = now.minusDays(period.lengthDays());
        long doneTotal = orderInfoService.countOrdersByFiltersInPeriod(
                buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(userId, from, now, null))
        );
        long doneSeparate = orderInfoService.countOrdersByFiltersInPeriod(
                buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(userId, from, now, "SEPARATE"))
        );

        return new EcoDashboard.OrdersStats(
                doneTotal,
                doneSeparate,
                calculateSharePercent(doneSeparate, doneTotal)
        );
    }

    private List<String> buildFractions(long userId, EcoDashboardPeriod period, OffsetDateTime now) {
        if (period == EcoDashboardPeriod.ALL) {
            return orderInfoService.findDistinctFractionNamesByFiltersInPeriod(
                    buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(
                            userId,
                            ALL_PERIOD_START,
                            now,
                            "SEPARATE"
                    ))
            );
        }

        OffsetDateTime from = now.minusDays(period.lengthDays());
        return orderInfoService.findDistinctFractionNamesByFiltersInPeriod(
                buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(userId, from, now, "SEPARATE"))
        );
    }

    private List<String> buildInsights(BuildInsightsParams params) {
        if (params.period() == EcoDashboardPeriod.ALL) {
            return buildAllTimeInsights(params.currentStats());
        }

        OffsetDateTime currentFrom = params.now().minusDays(params.period().lengthDays());
        OffsetDateTime prevFrom = currentFrom.minusDays(params.period().lengthDays());
        OffsetDateTime prevTo = currentFrom.minusNanos(1L);

        long prevDoneTotal = orderInfoService.countOrdersByFiltersInPeriod(
                buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(
                        params.userId(),
                        prevFrom,
                        prevTo,
                        null
                ))
        );
        long prevDoneSeparate = orderInfoService.countOrdersByFiltersInPeriod(
                buildDoneFilters(ecoDashboardParamsMapper.mapToBuildDoneFiltersParams(
                        params.userId(),
                        prevFrom,
                        prevTo,
                        "SEPARATE"
                ))
        );
        int prevSharePercent = calculateSharePercent(prevDoneSeparate, prevDoneTotal);

        return buildPeriodInsights(params.currentStats(), prevDoneSeparate, prevSharePercent);
    }

    private List<String> buildAllTimeInsights(EcoDashboard.OrdersStats currentStats) {
        if (currentStats.doneSeparate() == 0L) {
            return List.of(NO_SEPARATE_ORDERS_MESSAGE);
        }

        return List.of(
                ALL_TIME_SEPARATE_ORDERS_PREFIX + currentStats.doneSeparate() + ".",
                SEPARATE_ORDERS_SHARE_PREFIX + currentStats.separateSharePercent() + "%."
        );
    }

    private List<String> buildPeriodInsights(
            EcoDashboard.OrdersStats currentStats,
            long prevDoneSeparate,
            int prevSharePercent
    ) {
        long currentDoneSeparate = currentStats.doneSeparate();
        if (currentDoneSeparate == 0L && prevDoneSeparate == 0L) {
            return List.of(NO_SEPARATE_ORDERS_MESSAGE);
        }

        List<String> insights = new ArrayList<>();

        long delta = currentDoneSeparate - prevDoneSeparate;
        if (delta > 0L) {
            insights.add(
                    PERIOD_SEPARATE_ORDERS_PREFIX + currentDoneSeparate
                            + ". \u042d\u0442\u043e \u043d\u0430 +" + delta + PERIOD_MORE_SUFFIX
            );
        } else if (delta < 0L) {
            insights.add(
                    PERIOD_SEPARATE_ORDERS_PREFIX + currentDoneSeparate
                            + ". \u042d\u0442\u043e \u043d\u0430 " + Math.abs(delta) + PERIOD_LESS_SUFFIX
            );
        } else {
            insights.add(PERIOD_SEPARATE_ORDERS_PREFIX + currentDoneSeparate + PERIOD_EQUAL_SUFFIX);
        }

        int shareDelta = currentStats.separateSharePercent() - prevSharePercent;
        if (shareDelta > 0) {
            insights.add(
                    SHARE_GROWTH_PREFIX + shareDelta + PP_SUFFIX
                            + currentStats.separateSharePercent() + AGAINST_SUFFIX + prevSharePercent + "%."
            );
        } else if (shareDelta < 0) {
            insights.add(
                    SHARE_DECLINE_PREFIX + Math.abs(shareDelta) + PP_SUFFIX
                            + currentStats.separateSharePercent() + AGAINST_SUFFIX + prevSharePercent + "%."
            );
        } else {
            insights.add(SHARE_UNCHANGED_PREFIX + currentStats.separateSharePercent() + "%.");
        }

        return insights;
    }

    private EcoDashboard.AchieverProgress buildAchieverProgress(
            long userId,
            UserType userType,
            long totalPoints
    ) {
        if (userType != UserType.ACHIEVER) {
            return null;
        }

        int levelId = achieverProfileRepository.findCurrentLevelId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Achiever profile is not found for user id = %s".formatted(userId)
                ));

        Optional<Level> nextLevel = levelRepository.findNextTargetLevelByTotalPoints(totalPoints);
        if (nextLevel.isEmpty()) {
            return new EcoDashboard.AchieverProgress(
                    levelId,
                    null,
                    0L,
                    FULL_PROGRESS_PERCENT,
                    true
            );
        }

        long required = nextLevel.get().requiredTotalPoints();
        long remaining = Math.max(0L, required - totalPoints);

        return new EcoDashboard.AchieverProgress(
                levelId,
                required,
                remaining,
                calculateProgressPercent(totalPoints, required),
                false
        );
    }

    private EcoDashboard.Period buildPeriod(EcoDashboardPeriod selected) {
        List<EcoDashboard.PeriodOption> options = List.of(
                toPeriodOption(EcoDashboardPeriod.WEEK, selected),
                toPeriodOption(EcoDashboardPeriod.MONTH, selected),
                toPeriodOption(EcoDashboardPeriod.ALL, selected)
        );

        return new EcoDashboard.Period(
                selected.name(),
                selected.title(),
                options
        );
    }

    private EcoDashboard.PeriodOption toPeriodOption(
            EcoDashboardPeriod option,
            EcoDashboardPeriod selected
    ) {
        return new EcoDashboard.PeriodOption(
                option.name(),
                option.title(),
                option == selected
        );
    }

    private void addEcoProfileOpenedEvent(long userId, EcoDashboardPeriod period) {
        userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                userId,
                UserActionEventType.ECO_PROFILE_OPENED.dbName(),
                toJson(Map.of("period", period.name())),
                ZERO_POINTS_DIFFERENCE
        ));
    }

    private int calculateSharePercent(long separateDone, long doneTotal) {
        if (doneTotal <= 0L) {
            return 0;
        }
        return (int) ((Math.max(0L, separateDone) * FULL_PROGRESS_PERCENT) / doneTotal);
    }

    private int calculateProgressPercent(long totalPoints, long requiredPoints) {
        if (requiredPoints <= 0L) {
            return FULL_PROGRESS_PERCENT;
        }

        long safeTotalPoints = Math.max(0L, totalPoints);
        long rawPercent = (safeTotalPoints * FULL_PROGRESS_PERCENT) / requiredPoints;
        return (int) Math.max(0L, Math.min(FULL_PROGRESS_PERCENT, rawPercent));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize event content", exception);
        }
    }

    private OrderFiltersInPeriod buildDoneFilters(BuildDoneFiltersParams params) {
        return new OrderFiltersInPeriod(
                params.userId(),
                params.from(),
                params.to(),
                "DONE",
                params.type(),
                null
        );
    }
}
