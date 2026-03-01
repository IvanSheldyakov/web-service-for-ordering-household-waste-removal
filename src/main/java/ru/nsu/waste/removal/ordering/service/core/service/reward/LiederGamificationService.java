package ru.nsu.waste.removal.ordering.service.core.service.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.LiederRewardEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.LiederRewardRequestEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventHandler;

@Service
@Order(15)
@RequiredArgsConstructor
public class LiederGamificationService implements UserActionEventHandler {

    private static final long HABIT_STRENGTH_SCALE = 1_000_000L;
    private static final double DEFAULT_ALPHA = 0.10;
    private static final double DEFAULT_THETA = 0.90;
    private static final int DEFAULT_MAX_POINTS = 50;

    private static final LiederOptimizedRewardCalculator CALCULATOR =
            new LiederOptimizedRewardCalculator(DEFAULT_ALPHA, DEFAULT_THETA, DEFAULT_MAX_POINTS);

    private final UserInfoRepository userInfoRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.pointsDifference() == 0L && isSupportedRewardEventType(event.eventType());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {
        boolean success = extractSuccess(event.content());

        UserRewardState state = userInfoRepository.findRewardStateForUpdate(event.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "User with id = %s is not found".formatted(event.userId())
                ));

        double oldStrength = fromDbStrength(state.habitStrength());
        LiederOptimizedRewardCalculator.StepResult step = CALCULATOR.step(oldStrength, success);

        long calculatedDelta = step.pointsDelta();
        long newTotalPoints = state.totalPoints();
        long newCurrentPoints = state.currentPoints();
        long appliedDelta;

        if (calculatedDelta > 0) {
            appliedDelta = calculatedDelta;
            newTotalPoints = safeAdd(newTotalPoints, appliedDelta);
            newCurrentPoints = safeAdd(newCurrentPoints, appliedDelta);
        } else if (calculatedDelta < 0) {
            long wantToSubtract = -calculatedDelta;
            long canSubtract = Math.min(newCurrentPoints, wantToSubtract);
            appliedDelta = -canSubtract;
            newCurrentPoints = newCurrentPoints - canSubtract;
        } else {
            appliedDelta = 0;
        }

        long newHabitStrength = toDbStrength(step.newStrength());
        userInfoRepository.updateRewardState(event.userId(), newTotalPoints, newCurrentPoints, newHabitStrength);

        String rewardContentJson = buildRewardContentJson(
                success,
                oldStrength,
                step.newStrength(),
                step.fValue(),
                calculatedDelta,
                appliedDelta
        );

        userActionHistoryRepository.updateEventRewardById(
                event.id(),
                rewardContentJson,
                appliedDelta
        );
    }

    private static double fromDbStrength(long habitStrength) {
        return clamp01(habitStrength / (double) HABIT_STRENGTH_SCALE);
    }

    private static long toDbStrength(double habitStrength) {
        return Math.round(clamp01(habitStrength) * HABIT_STRENGTH_SCALE);
    }

    private static double clamp01(double x) {
        if (x < 0.0) {
            return 0.0;
        }
        if (x > 1.0) {
            return 1.0;
        }
        return x;
    }

    private static long safeAdd(long a, long b) {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0) {
            throw new ArithmeticException("long overflow during points update");
        }
        return result;
    }

    private String buildRewardContentJson(
            boolean success,
            double oldStrength,
            double newStrength,
            double fValue,
            long calculatedDelta,
            long appliedDelta
    ) {
        LiederRewardEventContent content = new LiederRewardEventContent(
                "LIEDER_OPTIMIZED",
                DEFAULT_ALPHA,
                DEFAULT_THETA,
                DEFAULT_MAX_POINTS,
                success,
                oldStrength,
                newStrength,
                fValue,
                calculatedDelta,
                appliedDelta
        );

        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize reward content", exception);
        }
    }

    private boolean extractSuccess(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return true;
        }
        try {
            LiederRewardRequestEventContent content =
                    objectMapper.readValue(contentJson, LiederRewardRequestEventContent.class);
            return content.success();
        } catch (Exception ignored) {
            return true;
        }
    }

    private boolean isSupportedRewardEventType(UserActionEventType eventType) {
        return eventType == UserActionEventType.SEPARATE_CHOSEN
                || eventType == UserActionEventType.GREEN_SLOT_CHOSEN;
    }
}
