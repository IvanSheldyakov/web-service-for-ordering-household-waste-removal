package ru.nsu.waste.removal.ordering.service.core.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderPaidWithPointsEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderPaymentStatus;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private static final String INSUFFICIENT_POINTS_MESSAGE = "Недостаточно баллов для оплаты заказа";

    private final UserInfoRepository userInfoRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;

    public UserRewardState lockRewardStateForPointsPayment(long userId, long costPoints) {
        validateCostPoints(costPoints);
        UserRewardState rewardState = userInfoRepository.findRewardStateForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User with id = %s is not found".formatted(userId)
                ));
        if (rewardState.currentPoints() < costPoints) {
            throw new IllegalStateException(INSUFFICIENT_POINTS_MESSAGE);
        }
        return rewardState;
    }

    public void payForOrderWithPoints(
            long userId,
            UserRewardState lockedRewardState,
            OrderKey orderKey,
            OrderType type,
            SlotOption slot,
            List<Long> fractionIds,
            long costPoints
    ) {
        validateCostPoints(costPoints);
        if (lockedRewardState == null || lockedRewardState.userId() != userId) {
            throw new IllegalStateException("Reward state for payment is not locked for user");
        }
        if (lockedRewardState.currentPoints() < costPoints) {
            throw new IllegalStateException(INSUFFICIENT_POINTS_MESSAGE);
        }

        long newCurrentPoints = safeSubtract(lockedRewardState.currentPoints(), costPoints);
        userInfoRepository.updateRewardState(
                userId,
                lockedRewardState.totalPoints(),
                newCurrentPoints,
                lockedRewardState.habitStrength()
        );

        List<Long> safeFractionIds = fractionIds == null ? List.of() : List.copyOf(fractionIds);
        OrderPaidWithPointsEventContent content = new OrderPaidWithPointsEventContent(
                orderKey.id(),
                type.dbName(),
                slot.green(),
                slot.pickupFrom().toString(),
                slot.pickupTo().toString(),
                safeFractionIds,
                costPoints,
                OrderPaymentStatus.PAID_WITH_POINTS.dbName()
        );
        userActionHistoryRepository.addEvent(
                userId,
                UserActionEventType.ORDER_PAID_WITH_POINTS.dbName(),
                toJson(content),
                safeNegate(costPoints)
        );
    }

    private void validateCostPoints(long costPoints) {
        if (costPoints <= 0L) {
            throw new IllegalStateException("Order cost in points must be positive");
        }
    }

    private long safeSubtract(long value, long delta) {
        if (delta < 0L || value < delta) {
            throw new IllegalStateException(INSUFFICIENT_POINTS_MESSAGE);
        }
        return value - delta;
    }

    private long safeNegate(long value) {
        if (value == Long.MIN_VALUE) {
            throw new ArithmeticException("long overflow during points update");
        }
        return -value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize event content", exception);
        }
    }
}

