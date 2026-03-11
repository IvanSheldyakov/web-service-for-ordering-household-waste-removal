package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import ru.nsu.waste.removal.ordering.service.app.form.OrderCreateForm;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.order.WasteFractionRepository;
import ru.nsu.waste.removal.ordering.service.core.service.order.GreenSlotService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderCreateCommand;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderCreateService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderPricingService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreateFacadeTest {

    @Mock
    private WasteFractionRepository wasteFractionRepository;

    @Mock
    private GreenSlotService greenSlotService;

    @Mock
    private OrderCreateService orderCreateService;

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private OrderPricingService orderPricingService;

    @InjectMocks
    private OrderCreateFacade orderCreateFacade;

    @Test
    void createOrder_passesPayWithPointsFlagToServiceCommand() {
        long userId = 42L;
        OrderCreateForm form = new OrderCreateForm();
        form.setType("MIXED");
        form.setSlotKey("2026-03-20T10:00:00+00:00|2026-03-20T12:00:00+00:00");
        form.setPayWithPoints(true);

        when(orderCreateService.createOrder(userId, new OrderCreateCommand(
                form.getType(),
                form.getSlotKey(),
                form.getFractionIds(),
                form.isPayWithPoints()
        ))).thenReturn(101L);

        orderCreateFacade.createOrder(userId, form);

        ArgumentCaptor<OrderCreateCommand> commandCaptor = ArgumentCaptor.forClass(OrderCreateCommand.class);
        verify(orderCreateService).createOrder(org.mockito.ArgumentMatchers.eq(userId), commandCaptor.capture());
        assertTrue(commandCaptor.getValue().payWithPoints());
    }

    @Test
    void validate_whenPointsAreInsufficient_addsFieldError() {
        long userId = 55L;
        SlotOption slot = new SlotOption(
                OffsetDateTime.parse("2026-03-20T10:00:00+00:00"),
                OffsetDateTime.parse("2026-03-20T12:00:00+00:00"),
                false
        );
        OrderCreateForm form = new OrderCreateForm();
        form.setType("MIXED");
        form.setSlotKey(slot.key());
        form.setPayWithPoints(true);

        when(greenSlotService.getSlotOptions(userId)).thenReturn(List.of(slot));
        when(userInfoService.getProfileByUserId(userId)).thenReturn(
                new UserProfileInfo(userId, UserType.ACHIEVER, 300L, 40L, "630000")
        );
        when(orderPricingService.getFixedCostPoints()).thenReturn(100L);

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "orderCreateForm");
        orderCreateFacade.validate(form, bindingResult, userId);

        assertTrue(bindingResult.hasFieldErrors("payWithPoints"));
    }

    @Test
    void getPaymentInfo_returnsCurrentAndCostFromServices() {
        long userId = 99L;
        when(userInfoService.getProfileByUserId(userId)).thenReturn(
                new UserProfileInfo(userId, UserType.EXPLORER, 100L, 70L, "630000")
        );
        when(orderPricingService.getFixedCostPoints()).thenReturn(60L);

        assertEquals(70L, orderCreateFacade.getCurrentPoints(userId));
        assertEquals(60L, orderCreateFacade.getFixedCostPoints());
        assertTrue(orderCreateFacade.hasEnoughPointsForPayment(userId));
    }
}

