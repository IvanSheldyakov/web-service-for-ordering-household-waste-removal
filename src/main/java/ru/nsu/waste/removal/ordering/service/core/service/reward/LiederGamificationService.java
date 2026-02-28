package ru.nsu.waste.removal.ordering.service.core.service.reward;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.LiederRewardEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.reward.RewardApplicationResult;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;

@Service
@RequiredArgsConstructor
public class LiederGamificationService {

    /**
     * В БД habit_strength хранится как long, поэтому используем fixed-point шкалу.
     * 1_000_000 = точность до 6 знаков после запятой.
     */
    private static final long HABIT_STRENGTH_SCALE = 1_000_000L;

    /**
     * Параметры по умолчанию для MVP.
     * <p>
     * alpha=0.10 и theta=0.90 — типовые значения из статьи.
     * M (maxPoints) масштабируем под экономику баллов в проекте.
     */
    private static final double DEFAULT_ALPHA = 0.10;
    private static final double DEFAULT_THETA = 0.90;
    private static final int DEFAULT_MAX_POINTS = 50;

    private static final LiederOptimizedRewardCalculator CALCULATOR =
            new LiederOptimizedRewardCalculator(DEFAULT_ALPHA, DEFAULT_THETA, DEFAULT_MAX_POINTS);

    private final UserInfoRepository userInfoRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;

    /**
     * Применить оптимизированную награду (модель Лидера) к пользователю.
     * <p>
     * Когда вызывать:
     * - SEPARATE_CHOSEN: пользователь выбрал раздельный вывоз
     * - GREEN_SLOT_CHOSEN: пользователь выбрал "зелёный" слот
     * - ECO_TASK_COMPLETED: пользователь выполнил эко-задание
     */
    @Transactional
    public RewardApplicationResult apply(long userId, UserActionEventType eventType, boolean success) {
        UserRewardState state = userInfoRepository.findRewardStateForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Пользователь с id = %s не найден".formatted(userId)
                ));

        double oldStrength = fromDbStrength(state.habitStrength());

        LiederOptimizedRewardCalculator.StepResult step = CALCULATOR.step(oldStrength, success);

        long calculatedDelta = step.pointsDelta();

        // total_points — накопленный вклад (обычно не уменьшаем)
        // current_points — текущий баланс (может уменьшаться, но не < 0)
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
        userInfoRepository.updateRewardState(userId, newTotalPoints, newCurrentPoints, newHabitStrength);

        String contentJson = buildContentJson(
                success,
                oldStrength,
                step.newStrength(),
                step.fValue(),
                calculatedDelta,
                appliedDelta
        );
        userActionHistoryRepository.addEvent(userId, eventType.dbName(), contentJson, appliedDelta);

        return new RewardApplicationResult(
                userId,
                eventType,
                success,
                appliedDelta,
                (int) calculatedDelta,
                newTotalPoints,
                newCurrentPoints,
                step.newStrength()
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
        long r = a + b;
        if (((a ^ r) & (b ^ r)) < 0) {
            throw new ArithmeticException("long overflow during points update");
        }
        return r;
    }

    private String buildContentJson(
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
}
