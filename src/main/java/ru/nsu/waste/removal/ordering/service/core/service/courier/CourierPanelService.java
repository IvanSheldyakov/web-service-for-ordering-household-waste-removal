package ru.nsu.waste.removal.ordering.service.core.service.courier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderDoneEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.CourierOrderRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.CourierRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierPanelService {

    private static final int DEFAULT_ORDERS_LIMIT = 50;
    private static final long COURIER_DONE_ORDER_REWARD = 20L;
    private static final long ZERO_POINTS_DIFFERENCE = 0L;

    private static final String DONE_STATUS = "DONE";

    private static final String TAKE_ORDER_FAILED_MESSAGE = "Заказ уже взят другим курьером или недоступен";
    private static final String COMPLETE_ORDER_NOT_FOUND_MESSAGE =
            "Заказ не найден или не назначен этому курьеру";
    private static final String COMPLETE_ORDER_FAILED_MESSAGE = "Не удалось завершить заказ";
    private static final String COURIER_POINTS_UPDATE_FAILED_MESSAGE = "Не удалось начислить баллы курьеру";

    private final CourierInfoService courierInfoService;
    private final CourierRepository courierRepository;
    private final CourierOrderRepository courierOrderRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public CourierPanel getPanel(long courierId) {
        CourierProfileInfo profile = courierInfoService.getProfile(courierId);
        List<CourierOrderInfo> availableOrders =
                courierOrderRepository.findAvailableOrdersByCourierId(courierId, DEFAULT_ORDERS_LIMIT);
        List<CourierOrderInfo> assignedOrders =
                courierOrderRepository.findAssignedOrdersByCourierId(courierId, DEFAULT_ORDERS_LIMIT);

        return new CourierPanel(
                profile.courierId(),
                profile.fullName(),
                profile.postalCode(),
                profile.totalPoints(),
                availableOrders,
                assignedOrders
        );
    }

    @Transactional
    public void takeOrder(long courierId, OrderKey orderKey) {
        CourierProfileInfo courierProfile = courierInfoService.getProfile(courierId);
        boolean taken = courierOrderRepository.takeOrder(
                courierId,
                orderKey.id(),
                orderKey.createdAt(),
                courierProfile.postalCode(),
                OffsetDateTime.now(clock)
        );

        if (!taken) {
            throw new IllegalStateException(TAKE_ORDER_FAILED_MESSAGE);
        }
    }

    @Transactional
    public void completeOrder(long courierId, OrderKey orderKey) {
        CourierOrderInfo assignedOrder = courierOrderRepository.findAssignedOrderForCompletion(
                courierId,
                orderKey.id(),
                orderKey.createdAt()
        ).orElseThrow(() -> new IllegalStateException(COMPLETE_ORDER_NOT_FOUND_MESSAGE));

        boolean done = courierOrderRepository.markDone(
                courierId,
                orderKey.id(),
                orderKey.createdAt(),
                OffsetDateTime.now(clock)
        );

        if (!done) {
            throw new IllegalStateException(COMPLETE_ORDER_FAILED_MESSAGE);
        }

        boolean pointsUpdated = courierRepository.addPoints(courierId, COURIER_DONE_ORDER_REWARD);
        if (!pointsUpdated) {
            throw new IllegalStateException(COURIER_POINTS_UPDATE_FAILED_MESSAGE);
        }

        List<Long> fractionIds = courierOrderRepository.findFractionIds(orderKey.id(), orderKey.createdAt());
        OrderDoneEventContent content = new OrderDoneEventContent(
                assignedOrder.orderId(),
                assignedOrder.type(),
                assignedOrder.greenChosen(),
                asString(assignedOrder.pickupFrom()),
                asString(assignedOrder.pickupTo()),
                fractionIds,
                courierId,
                DONE_STATUS
        );

        userActionHistoryRepository.addEvent(
                assignedOrder.userId(),
                UserActionEventType.ORDER_DONE.dbName(),
                toJson(content),
                ZERO_POINTS_DIFFERENCE
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize event content", exception);
        }
    }

    private String asString(OffsetDateTime value) {
        return value == null ? null : value.toString();
    }
}
