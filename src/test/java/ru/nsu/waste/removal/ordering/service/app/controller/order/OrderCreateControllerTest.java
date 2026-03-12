package ru.nsu.waste.removal.ordering.service.app.controller.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.form.OrderCreateForm;
import ru.nsu.waste.removal.ordering.service.core.facade.OrderCreateFacade;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.model.order.WasteFraction;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCreateControllerTest {

    @Mock
    private OrderCreateFacade orderCreateFacade;

    @InjectMocks
    private OrderCreateController orderCreateController;

    @Test
    void getOrderCreate_populatesFormPaymentAndReferenceData() {
        long userId = 77L;
        when(orderCreateFacade.getActiveFractions()).thenReturn(List.of(
                new WasteFraction(1L, "PLASTIC", "РџР»Р°СЃС‚РёРє", true)
        ));
        when(orderCreateFacade.getSlotOptions(userId)).thenReturn(List.of(
                new SlotOption(
                        OffsetDateTime.parse("2026-03-20T10:00:00+00:00"),
                        OffsetDateTime.parse("2026-03-20T12:00:00+00:00"),
                        false
                )
        ));
        when(orderCreateFacade.getCurrentPoints(userId)).thenReturn(150L);
        when(orderCreateFacade.getFixedCostPoints()).thenReturn(100L);
        when(orderCreateFacade.hasEnoughPointsForPayment(userId)).thenReturn(true);

        Model model = new ExtendedModelMap();
        String view = orderCreateController.getOrderCreate(userId, model);

        assertEquals(TemplateNames.ORDER_CREATE, view);
        assertNotNull(model.getAttribute("orderCreateForm"));
        assertEquals(150L, model.getAttribute("currentPoints"));
        assertEquals(100L, model.getAttribute("fixedCostPoints"));
        assertEquals(true, model.getAttribute("enoughPointsForPayment"));
    }

    @Test
    void createOrder_whenValidationFails_returnsCreateTemplate() {
        long userId = 88L;
        OrderCreateForm form = new OrderCreateForm();
        form.setType("MIXED");
        form.setSlotKey("2026-03-20T10:00:00+00:00|2026-03-20T12:00:00+00:00");
        form.setPayWithPoints(true);
        Model model = new ExtendedModelMap();

        doAnswer(invocation -> {
            BindingResult result = invocation.getArgument(1);
            result.rejectValue("payWithPoints", "insufficient", "РќРµРґРѕСЃС‚Р°С‚РѕС‡РЅРѕ Р±Р°Р»Р»РѕРІ РґР»СЏ РѕРїР»Р°С‚С‹ Р·Р°РєР°Р·Р°");
            return null;
        }).when(orderCreateFacade).validate(eq(form), any(BindingResult.class), eq(userId));
        when(orderCreateFacade.getActiveFractions()).thenReturn(List.of());
        when(orderCreateFacade.getSlotOptions(userId)).thenReturn(List.of());
        when(orderCreateFacade.getCurrentPoints(userId)).thenReturn(20L);
        when(orderCreateFacade.getFixedCostPoints()).thenReturn(100L);
        when(orderCreateFacade.hasEnoughPointsForPayment(userId)).thenReturn(false);

        String view = orderCreateController.createOrder(userId, form, model);

        assertEquals(TemplateNames.ORDER_CREATE, view);
        verify(orderCreateFacade, never()).createOrder(userId, form);
    }

    @Test
    void createOrder_whenSuccessful_redirectsToHome() {
        long userId = 90L;
        OrderCreateForm form = new OrderCreateForm();
        form.setType("MIXED");
        form.setSlotKey("2026-03-20T10:00:00+00:00|2026-03-20T12:00:00+00:00");
        Model model = new ExtendedModelMap();

        String view = orderCreateController.createOrder(userId, form, model);

        assertEquals("redirect:/user/90/home", view);
        verify(orderCreateFacade).createOrder(userId, form);
    }
}
