package ru.nsu.waste.removal.ordering.service.core.service.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderCreatedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderCreateParams;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderPaymentStatus;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.order.OrderCreateRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.order.WasteFractionRepository;
import ru.nsu.waste.removal.ordering.service.core.service.cluster.GeoClusterService;
import ru.nsu.waste.removal.ordering.service.core.service.order.mapper.OrderCreateParamsMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCreateService {

    private static final long ZERO_POINTS_DIFFERENCE = 0L;
    private static final String INVALID_TYPE_MESSAGE = "Некорректный тип вывоза";
    private static final String INVALID_SLOT_KEY_MESSAGE = "Выбранный слот недоступен";
    private static final String INVALID_FRACTIONS_MESSAGE = "Некорректный выбор фракций";

    private final GreenSlotService greenSlotService;
    private final GeoClusterService geoClusterService;
    private final WasteFractionRepository wasteFractionRepository;
    private final OrderCreateRepository orderCreateRepository;
    private final OrderCreateParamsMapper orderCreateParamsMapper;
    private final OrderPricingService orderPricingService;
    private final OrderPaymentService orderPaymentService;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public long createOrder(long userId, OrderCreateCommand command) {
        OrderType type = resolveOrderType(command.type());
        SlotOption selectedSlot = resolveSelectedSlot(userId, command.slotKey());
        List<Long> fractionIds = normalizeFractions(command.fractionIds(), type);
        long costPoints = orderPricingService.getFixedCostPoints();

        UserRewardState rewardStateForPayment = null;
        OrderPaymentStatus paymentStatus = OrderPaymentStatus.UNPAID;
        if (command.payWithPoints()) {
            rewardStateForPayment = orderPaymentService.lockRewardStateForPointsPayment(userId, costPoints);
            paymentStatus = OrderPaymentStatus.PAID_WITH_POINTS;
        }

        GeoClusterKey clusterKey = geoClusterService.getClusterKeyForOrderCreation(userId);
        OrderCreateParams orderCreateParams = orderCreateParamsMapper.toOrderCreateParams(
                userId,
                type,
                selectedSlot,
                clusterKey,
                costPoints,
                paymentStatus
        );
        OrderKey orderKey = orderCreateRepository.createOrder(orderCreateParams);

        if (type == OrderType.SEPARATE) {
            orderCreateRepository.addFractions(orderKey, fractionIds);
        }

        addOrderCreatedEvent(userId, orderKey, type, selectedSlot, fractionIds);

        if (command.payWithPoints()) {
            orderPaymentService.payForOrderWithPoints(
                    userId,
                    rewardStateForPayment,
                    orderKey,
                    type,
                    selectedSlot,
                    fractionIds,
                    costPoints
            );
        }

        if (type == OrderType.SEPARATE) {
            addSuccessEvent(userId, UserActionEventType.SEPARATE_CHOSEN);
        }
        if (selectedSlot.green()) {
            addSuccessEvent(userId, UserActionEventType.GREEN_SLOT_CHOSEN);
        }

        return orderKey.id();
    }

    private OrderType resolveOrderType(String type) {
        return OrderType.tryFrom(type)
                .orElseThrow(() -> new IllegalStateException(INVALID_TYPE_MESSAGE));
    }

    private List<Long> normalizeFractions(List<Long> fractionIds, OrderType type) {
        if (type == OrderType.MIXED) {
            return List.of();
        }
        if (fractionIds == null || fractionIds.isEmpty()) {
            throw new IllegalStateException(INVALID_FRACTIONS_MESSAGE);
        }
        if (fractionIds.stream().anyMatch(id -> id == null || id <= 0L)) {
            throw new IllegalStateException(INVALID_FRACTIONS_MESSAGE);
        }
        long uniqueSize = fractionIds.stream().distinct().count();
        if (uniqueSize != fractionIds.size()) {
            throw new IllegalStateException(INVALID_FRACTIONS_MESSAGE);
        }

        int activeCount = wasteFractionRepository.findActiveFractionsByIds(fractionIds).size();
        if (activeCount != fractionIds.size()) {
            throw new IllegalStateException(INVALID_FRACTIONS_MESSAGE);
        }
        return List.copyOf(fractionIds);
    }

    private SlotOption resolveSelectedSlot(long userId, String slotKey) {
        SlotBounds slotBounds = parseSlotKey(slotKey);
        return greenSlotService.getSlotOptions(userId).stream()
                .filter(slot -> matchesSlot(slot, slotBounds))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(INVALID_SLOT_KEY_MESSAGE));
    }

    private SlotBounds parseSlotKey(String slotKey) {
        if (slotKey == null || slotKey.isBlank()) {
            throw new IllegalStateException(INVALID_SLOT_KEY_MESSAGE);
        }
        String[] parts = slotKey.trim().split("\\|", -1);
        if (parts.length != 2) {
            throw new IllegalStateException(INVALID_SLOT_KEY_MESSAGE);
        }
        try {
            OffsetDateTime pickupFrom = OffsetDateTime.parse(parts[0]);
            OffsetDateTime pickupTo = OffsetDateTime.parse(parts[1]);
            return new SlotBounds(pickupFrom, pickupTo);
        } catch (Exception exception) {
            throw new IllegalStateException(INVALID_SLOT_KEY_MESSAGE);
        }
    }

    private boolean matchesSlot(SlotOption slot, SlotBounds slotBounds) {
        return slot.pickupFrom().toInstant().equals(slotBounds.pickupFrom().toInstant())
                && slot.pickupTo().toInstant().equals(slotBounds.pickupTo().toInstant());
    }

    private void addOrderCreatedEvent(
            long userId,
            OrderKey orderKey,
            OrderType type,
            SlotOption slot,
            List<Long> fractionIds
    ) {
        OrderCreatedEventContent content = new OrderCreatedEventContent(
                orderKey.id(),
                type.dbName(),
                slot.pickupFrom().toString(),
                slot.pickupTo().toString(),
                slot.green(),
                fractionIds
        );

        userActionHistoryRepository.addEvent(
                userId,
                UserActionEventType.ORDER_CREATED.dbName(),
                toJson(content),
                ZERO_POINTS_DIFFERENCE
        );
    }

    private void addSuccessEvent(long userId, UserActionEventType eventType) {
        userActionHistoryRepository.addEvent(
                userId,
                eventType.dbName(),
                toJson(Map.of("success", true)),
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

    private record SlotBounds(
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo
    ) {
    }
}

