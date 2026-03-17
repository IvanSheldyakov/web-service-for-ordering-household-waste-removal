package ru.nsu.waste.removal.ordering.service.app.controller.courier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderGroupActionForm;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierFacade;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierControllerTest {

    @Mock
    private CourierFacade courierFacade;

    @Mock
    private Validator validator;

    @InjectMocks
    private CourierController courierController;

    @Test
    void takeOrderGroup_withValidForm_setsSuccessFlashAndRedirects() {
        long courierId = 77L;
        CourierOrderGroupActionForm form = new CourierOrderGroupActionForm();
        form.setClusterKey("630000");
        form.setPickupFrom(OffsetDateTime.parse("2026-03-21T10:00:00+07:00"));
        form.setPickupTo(OffsetDateTime.parse("2026-03-21T12:00:00+07:00"));
        form.setExpectedOrderCount(2);
        when(validator.validate(form)).thenReturn(Set.of());

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = courierController.takeOrderGroup(courierId, form, redirectAttributes);

        assertEquals("redirect:/courier/77/home", view);
        assertEquals(
                "В работу взято 2 заказов",
                redirectAttributes.getFlashAttributes().get(AttributeNames.SUCCESS_MESSAGE)
        );
        verify(courierFacade).takeOrderGroup(courierId, form);
    }

    @Test
    void takeOrderGroup_withValidationErrors_setsErrorFlashAndRedirects() {
        long courierId = 77L;
        CourierOrderGroupActionForm form = new CourierOrderGroupActionForm();
        when(validator.validate(form)).thenReturn(Set.of(mock(ConstraintViolation.class)));

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = courierController.takeOrderGroup(courierId, form, redirectAttributes);

        assertEquals("redirect:/courier/77/home", view);
        assertEquals(
                "Некорректные данные группы заказов",
                redirectAttributes.getFlashAttributes().get(AttributeNames.ERROR_MESSAGE)
        );
        verifyNoInteractions(courierFacade);
    }
}
