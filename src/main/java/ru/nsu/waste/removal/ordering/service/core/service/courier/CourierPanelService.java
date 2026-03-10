package ru.nsu.waste.removal.ordering.service.core.service.courier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroup;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroupKey;
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourierPanelService {

    private static final int DEFAULT_ORDERS_LIMIT = 50;
    private static final long COURIER_DONE_ORDER_REWARD = 20L;
    private static final long ZERO_POINTS_DIFFERENCE = 0L;

    private static final String DONE_STATUS = "DONE";
    private static final String ORDER_TYPE_SEPARATE = "SEPARATE";
    private static final String ORDER_TYPE_MIXED = "MIXED";

    private static final String TAKE_ORDER_FAILED_MESSAGE = "Заказ уже взят другим курьером или недоступен";
    private static final String TAKE_GROUP_ACCESS_DENIED_MESSAGE = "Группа недоступна для этого курьера";
    private static final String TAKE_GROUP_CHANGED_MESSAGE = "Состав группы изменился, обновите страницу";
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
                groupOrdersByClusterAndSlot(availableOrders),
                groupOrdersByClusterAndSlot(assignedOrders)
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
    public void takeOrderGroup(long courierId, CourierOrderGroupKey groupKey, int expectedOrderCount) {
        if (expectedOrderCount < 1) {
            throw new IllegalStateException(TAKE_GROUP_CHANGED_MESSAGE);
        }

        CourierProfileInfo courierProfile = courierInfoService.getProfile(courierId);
        if (!Objects.equals(courierProfile.postalCode(), groupKey.clusterKey())) {
            throw new IllegalStateException(TAKE_GROUP_ACCESS_DENIED_MESSAGE);
        }

        int updatedRows = courierOrderRepository.takeOrderGroup(
                courierId,
                groupKey.clusterKey(),
                groupKey.pickupFrom(),
                groupKey.pickupTo(),
                OffsetDateTime.now(clock)
        );

        if (updatedRows != expectedOrderCount) {
            throw new IllegalStateException(TAKE_GROUP_CHANGED_MESSAGE);
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

    private List<CourierOrderGroup> groupOrdersByClusterAndSlot(List<CourierOrderInfo> orders) {
        if (orders == null || orders.isEmpty()) {
            return List.of();
        }

        List<CourierOrderInfo> sortedOrders = orders.stream()
                .sorted(Comparator
                        .comparing((CourierOrderInfo order) -> order.pickupFrom().toInstant())
                        .thenComparing(order -> order.pickupTo().toInstant())
                        .thenComparing(CourierOrderInfo::orderCreatedAt))
                .toList();

        Map<GroupingKey, List<CourierOrderInfo>> grouped = new LinkedHashMap<>();
        for (CourierOrderInfo order : sortedOrders) {
            GroupingKey key = new GroupingKey(
                    order.postalCode(),
                    order.pickupFrom().toInstant(),
                    order.pickupTo().toInstant()
            );
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(order);
        }

        return grouped.entrySet().stream()
                .map(this::toOrderGroup)
                .toList();
    }

    private CourierOrderGroup toOrderGroup(Map.Entry<GroupingKey, List<CourierOrderInfo>> entry) {
        List<CourierOrderInfo> orders = entry.getValue().stream()
                .sorted(Comparator.comparing(CourierOrderInfo::orderCreatedAt))
                .toList();

        int separateCount = countByType(orders, ORDER_TYPE_SEPARATE);
        int mixedCount = countByType(orders, ORDER_TYPE_MIXED);

        CourierOrderInfo firstOrder = orders.getFirst();

        return new CourierOrderGroup(
                entry.getKey().clusterKey(),
                firstOrder.pickupFrom(),
                firstOrder.pickupTo(),
                orders.size(),
                separateCount,
                mixedCount,
                orders
        );
    }

    private int countByType(List<CourierOrderInfo> orders, String type) {
        return (int) orders.stream()
                .filter(order -> order.type() != null)
                .filter(order -> order.type().trim().toUpperCase(Locale.ROOT).equals(type))
                .count();
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

    private record GroupingKey(
            String clusterKey,
            Instant pickupFrom,
            Instant pickupTo
    ) {
    }
}
