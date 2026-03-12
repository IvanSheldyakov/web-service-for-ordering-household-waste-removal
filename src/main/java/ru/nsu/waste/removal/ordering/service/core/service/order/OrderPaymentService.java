package ru.nsu.waste.removal.ordering.service.core.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.mapper.user.UserInfoParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderPaidWithPointsEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderPaymentStatus;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.service.order.param.PayForOrderWithPointsParams;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private static final String INSUFFICIENT_POINTS_MESSAGE = "Недостаточно баллов для оплаты заказа";

    private final UserInfoRepository userInfoRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserInfoParamsMapper userInfoParamsMapper;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
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

    public void payForOrderWithPoints(PayForOrderWithPointsParams params) {
        validateCostPoints(params.costPoints());
        if (params.lockedRewardState() == null || params.lockedRewardState().userId() != params.userId()) {
            throw new IllegalStateException("Reward state for payment is not locked for user");
        }
        if (params.lockedRewardState().currentPoints() < params.costPoints()) {
            throw new IllegalStateException(INSUFFICIENT_POINTS_MESSAGE);
        }

        long newCurrentPoints = safeSubtract(params.lockedRewardState().currentPoints(), params.costPoints());
        userInfoRepository.updateRewardState(userInfoParamsMapper.mapToUpdateRewardStateParams(
                params.userId(),
                params.lockedRewardState().totalPoints(),
                newCurrentPoints,
                params.lockedRewardState().habitStrength()
        ));

        List<Long> safeFractionIds = params.fractionIds() == null ? List.of() : List.copyOf(params.fractionIds());
        OrderPaidWithPointsEventContent content = new OrderPaidWithPointsEventContent(
                params.orderKey().id(),
                params.type().dbName(),
                params.slot().green(),
                params.slot().pickupFrom().toString(),
                params.slot().pickupTo().toString(),
                safeFractionIds,
                params.costPoints(),
                OrderPaymentStatus.PAID_WITH_POINTS.dbName()
        );
        userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                params.userId(),
                UserActionEventType.ORDER_PAID_WITH_POINTS.dbName(),
                toJson(content),
                safeNegate(params.costPoints())
        ));
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
