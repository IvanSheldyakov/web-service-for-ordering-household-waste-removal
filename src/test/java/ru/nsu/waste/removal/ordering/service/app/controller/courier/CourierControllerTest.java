package ru.nsu.waste.removal.ordering.service.app.controller.courier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderGroupActionForm;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierFacade;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CourierControllerTest {

    @Mock
    private CourierFacade courierFacade;

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

        BindingResult bindingResult = new BeanPropertyBindingResult(form, "courierOrderGroupActionForm");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = courierController.takeOrderGroup(courierId, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/courier/77/home", view);
        assertEquals("В работу взято 2 заказов", redirectAttributes.getFlashAttributes().get(AttributeNames.SUCCESS_MESSAGE));
        verify(courierFacade).takeOrderGroup(courierId, form);
    }

    @Test
    void takeOrderGroup_withBindingErrors_setsErrorFlashAndRedirects() {
        long courierId = 77L;
        CourierOrderGroupActionForm form = new CourierOrderGroupActionForm();
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "courierOrderGroupActionForm");
        bindingResult.rejectValue("clusterKey", "NotBlank", "Ключ кластера обязателен");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = courierController.takeOrderGroup(courierId, form, bindingResult, redirectAttributes);

        assertEquals("redirect:/courier/77/home", view);
        assertEquals(
                "Некорректные данные группы заказов",
                redirectAttributes.getFlashAttributes().get(AttributeNames.ERROR_MESSAGE)
        );
        verifyNoInteractions(courierFacade);
    }
}
