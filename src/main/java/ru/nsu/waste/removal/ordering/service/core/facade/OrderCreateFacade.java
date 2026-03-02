package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.form.OrderCreateForm;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.order.WasteFraction;
import ru.nsu.waste.removal.ordering.service.core.repository.order.WasteFractionRepository;
import ru.nsu.waste.removal.ordering.service.core.service.order.GreenSlotService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderCreateCommand;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderCreateService;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCreateFacade {

    private static final String TYPE_FIELD = "type";
    private static final String SLOT_KEY_FIELD = "slotKey";
    private static final String FRACTION_IDS_FIELD = "fractionIds";

    private static final String INVALID_TYPE_MESSAGE = "Некорректный тип вывоза";
    private static final String INVALID_SLOT_KEY_MESSAGE = "Выбранный слот недоступен";
    private static final String EMPTY_FRACTIONS_MESSAGE = "Выберите хотя бы одну фракцию";
    private static final String INVALID_FRACTIONS_MESSAGE = "Некорректный выбор фракций";

    private final WasteFractionRepository wasteFractionRepository;
    private final GreenSlotService greenSlotService;
    private final OrderCreateService orderCreateService;

    public List<WasteFraction> getActiveFractions() {
        return wasteFractionRepository.findActiveFractions();
    }

    public List<SlotOption> getSlotOptions(long userId) {
        return greenSlotService.getSlotOptions(userId).stream()
                .sorted(Comparator.comparing(SlotOption::green)
                        .reversed()
                        .thenComparing(SlotOption::pickupFrom))
                .toList();
    }

    public void validate(OrderCreateForm form, BindingResult bindingResult, long userId) {
        validateType(form, bindingResult);
        validateSlot(form, bindingResult, userId);
        validateFractions(form, bindingResult);
    }

    public long createOrder(long userId, OrderCreateForm form) {
        return orderCreateService.createOrder(
                userId,
                new OrderCreateCommand(
                        form.getType(),
                        form.getSlotKey(),
                        form.getFractionIds()
                )
        );
    }

    private void validateType(OrderCreateForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(TYPE_FIELD)) {
            return;
        }
        resolveOrderType(form.getType());
    }

    private void validateSlot(OrderCreateForm form, BindingResult bindingResult, long userId) {
        if (bindingResult.hasFieldErrors(SLOT_KEY_FIELD)) {
            return;
        }
        SlotBounds selectedSlot = parseSlot(form.getSlotKey());
        if (selectedSlot == null) {
            bindingResult.addError(new FieldError(
                    AttributeNames.ORDER_CREATE_FORM,
                    SLOT_KEY_FIELD,
                    INVALID_SLOT_KEY_MESSAGE
            ));
            return;
        }

        boolean available = getSlotOptions(userId).stream()
                .anyMatch(slot -> matchesSlot(slot, selectedSlot));
        if (!available) {
            bindingResult.addError(new FieldError(
                    AttributeNames.ORDER_CREATE_FORM,
                    SLOT_KEY_FIELD,
                    INVALID_SLOT_KEY_MESSAGE
            ));
        }
    }

    private void validateFractions(OrderCreateForm form, BindingResult bindingResult) {
        OrderType orderType = resolveOrderType(form.getType());
        if (orderType != OrderType.SEPARATE) {
            return;
        }
        if (form.getFractionIds() == null || form.getFractionIds().isEmpty()) {
            bindingResult.addError(new FieldError(
                    AttributeNames.ORDER_CREATE_FORM,
                    FRACTION_IDS_FIELD,
                    EMPTY_FRACTIONS_MESSAGE
            ));
            return;
        }
        if (form.getFractionIds().stream().anyMatch(id -> id == null || id <= 0L)) {
            bindingResult.addError(new FieldError(
                    AttributeNames.ORDER_CREATE_FORM,
                    FRACTION_IDS_FIELD,
                    INVALID_FRACTIONS_MESSAGE
            ));
            return;
        }

        long uniqueSize = form.getFractionIds().stream().distinct().count();
        int foundActive = wasteFractionRepository.findActiveFractionsByIds(form.getFractionIds()).size();
        if (uniqueSize != form.getFractionIds().size() || foundActive != form.getFractionIds().size()) {
            bindingResult.addError(new FieldError(
                    AttributeNames.ORDER_CREATE_FORM,
                    FRACTION_IDS_FIELD,
                    INVALID_FRACTIONS_MESSAGE
            ));
        }
    }

    private OrderType resolveOrderType(String type) {
        return OrderType.tryFrom(type).orElse(null);
    }

    private SlotBounds parseSlot(String slotKey) {
        if (slotKey == null || slotKey.isBlank()) {
            return null;
        }
        String[] parts = slotKey.trim().split("\\|", -1);
        if (parts.length != 2) {
            return null;
        }
        try {
            return new SlotBounds(
                    OffsetDateTime.parse(parts[0]),
                    OffsetDateTime.parse(parts[1])
            );
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean matchesSlot(SlotOption slot, SlotBounds selectedSlot) {
        return slot.pickupFrom().toInstant().equals(selectedSlot.pickupFrom().toInstant())
                && slot.pickupTo().toInstant().equals(selectedSlot.pickupTo().toInstant());
    }

    private record SlotBounds(
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo
    ) {
    }
}
