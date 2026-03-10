package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderActionForm;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderGroupActionForm;
import ru.nsu.waste.removal.ordering.service.app.view.CourierPanelViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroup;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroupKey;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierPanelService;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CourierFacade {

    private final CourierPanelService courierPanelService;
    private final CourierInfoService courierInfoService;

    public CourierPanelViewModel getPanel(long courierId) {
        CourierPanel panel = courierPanelService.getPanel(courierId);
        ZoneId courierZoneId = resolveCourierZoneId(courierId);

        return new CourierPanelViewModel(
                panel.courierId(),
                panel.fullName(),
                panel.postalCode(),
                panel.totalPoints(),
                panel.availableOrderGroups().stream()
                        .map(group -> toGroupViewModel(group, courierZoneId, true))
                        .toList(),
                panel.assignedOrderGroups().stream()
                        .map(group -> toGroupViewModel(group, courierZoneId, false))
                        .toList()
        );
    }

    public void takeOrder(long courierId, CourierOrderActionForm form) {
        courierPanelService.takeOrder(courierId, toOrderKey(form));
    }

    public void takeOrderGroup(long courierId, CourierOrderGroupActionForm form) {
        courierPanelService.takeOrderGroup(courierId, toOrderGroupKey(form), form.getExpectedOrderCount());
    }

    public void completeOrder(long courierId, CourierOrderActionForm form) {
        courierPanelService.completeOrder(courierId, toOrderKey(form));
    }

    private OrderKey toOrderKey(CourierOrderActionForm form) {
        if (form.getOrderId() == null || form.getOrderCreatedAt() == null) {
            throw new IllegalStateException("Некорректный ключ заказа");
        }
        return new OrderKey(form.getOrderId(), form.getOrderCreatedAt());
    }

    private CourierOrderGroupKey toOrderGroupKey(CourierOrderGroupActionForm form) {
        if (form.getClusterKey() == null || form.getClusterKey().isBlank()
                || form.getPickupFrom() == null
                || form.getPickupTo() == null
        ) {
            throw new IllegalStateException("Некорректный ключ группы заказов");
        }
        return new CourierOrderGroupKey(form.getClusterKey().trim(), form.getPickupFrom(), form.getPickupTo());
    }

    private CourierPanelViewModel.CourierOrderGroupViewModel toGroupViewModel(
            CourierOrderGroup group,
            ZoneId courierZoneId,
            boolean withTakeAction
    ) {
        int ordersCount = group.ordersCount();
        String takeActionLabel = withTakeAction ? buildTakeActionLabel(ordersCount) : "";

        return new CourierPanelViewModel.CourierOrderGroupViewModel(
                group.clusterKey(),
                convertToCourierTimezone(group.pickupFrom(), courierZoneId),
                convertToCourierTimezone(group.pickupTo(), courierZoneId),
                ordersCount,
                group.separateOrdersCount(),
                group.mixedOrdersCount(),
                takeActionLabel,
                group.orders().stream()
                        .map(order -> toOrderViewModel(order, courierZoneId))
                        .toList()
        );
    }

    private String buildTakeActionLabel(int ordersCount) {
        if (ordersCount <= 1) {
            return "Взять заказ";
        }
        return "Взять все %s заказов".formatted(ordersCount);
    }

    private CourierPanelViewModel.CourierOrderViewModel toOrderViewModel(
            CourierOrderInfo order,
            ZoneId courierZoneId
    ) {
        return new CourierPanelViewModel.CourierOrderViewModel(
                order.orderId(),
                order.orderCreatedAt(),
                order.city(),
                order.detailedAddress(),
                order.postalCode(),
                order.type(),
                localizeOrderStatus(order.status()),
                convertToCourierTimezone(order.pickupFrom(), courierZoneId),
                convertToCourierTimezone(order.pickupTo(), courierZoneId),
                order.greenChosen(),
                order.fractions()
        );
    }

    private ZoneId resolveCourierZoneId(long courierId) {
        String timezone = courierInfoService.getProfile(courierId).timezone();
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }

    private OffsetDateTime convertToCourierTimezone(OffsetDateTime value, ZoneId courierZoneId) {
        if (value == null) {
            return null;
        }
        return value.atZoneSameInstant(courierZoneId).toOffsetDateTime();
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
}
