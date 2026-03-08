package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderActionForm;
import ru.nsu.waste.removal.ordering.service.app.view.CourierPanelViewModel;
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
                panel.availableOrders().stream()
                        .map(order -> toViewModel(order, courierZoneId))
                        .toList(),
                panel.assignedOrders().stream()
                        .map(order -> toViewModel(order, courierZoneId))
                        .toList()
        );
    }

    public void takeOrder(long courierId, CourierOrderActionForm form) {
        courierPanelService.takeOrder(courierId, toOrderKey(form));
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

    private CourierPanelViewModel.CourierOrderViewModel toViewModel(
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
