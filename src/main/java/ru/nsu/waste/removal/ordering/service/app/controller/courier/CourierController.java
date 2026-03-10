package ru.nsu.waste.removal.ordering.service.app.controller.courier;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderActionForm;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderGroupActionForm;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierFacade;

@Controller
@RequiredArgsConstructor
public class CourierController {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final CourierFacade courierFacade;

    @GetMapping(Paths.COURIER_PANEL)
    public String getPanel(@PathVariable(Paths.COURIER_ID) long courierId, Model model) {
        model.addAttribute(AttributeNames.PANEL, courierFacade.getPanel(courierId));
        return TemplateNames.COURIER_PANEL;
    }

    @PostMapping(Paths.COURIER_TAKE_ORDER)
    public String takeOrder(
            @PathVariable(Paths.COURIER_ID) long courierId,
            @Valid @ModelAttribute CourierOrderActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(AttributeNames.ERROR_MESSAGE, "Некорректные данные заказа");
            return redirectToPanel(courierId);
        }

        try {
            courierFacade.takeOrder(courierId, form);
            redirectAttributes.addFlashAttribute(AttributeNames.SUCCESS_MESSAGE, "Заказ успешно взят в работу");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(AttributeNames.ERROR_MESSAGE, exception.getMessage());
        }

        return redirectToPanel(courierId);
    }

    @PostMapping(Paths.COURIER_TAKE_ORDER_GROUP)
    public String takeOrderGroup(
            @PathVariable(Paths.COURIER_ID) long courierId,
            @Valid @ModelAttribute CourierOrderGroupActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    AttributeNames.ERROR_MESSAGE,
                    "Некорректные данные группы заказов"
            );
            return redirectToPanel(courierId);
        }

        try {
            courierFacade.takeOrderGroup(courierId, form);
            String successMessage = form.getExpectedOrderCount() != null && form.getExpectedOrderCount() > 1
                    ? "В работу взято %s заказов".formatted(form.getExpectedOrderCount())
                    : "Заказ успешно взят в работу";
            redirectAttributes.addFlashAttribute(AttributeNames.SUCCESS_MESSAGE, successMessage);
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(AttributeNames.ERROR_MESSAGE, exception.getMessage());
        }

        return redirectToPanel(courierId);
    }

    @PostMapping(Paths.COURIER_COMPLETE_ORDER)
    public String completeOrder(
            @PathVariable(Paths.COURIER_ID) long courierId,
            @Valid @ModelAttribute CourierOrderActionForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(AttributeNames.ERROR_MESSAGE, "Некорректные данные заказа");
            return redirectToPanel(courierId);
        }

        try {
            courierFacade.completeOrder(courierId, form);
            redirectAttributes.addFlashAttribute(AttributeNames.SUCCESS_MESSAGE, "Заказ отмечен как выполненный");
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute(AttributeNames.ERROR_MESSAGE, exception.getMessage());
        }

        return redirectToPanel(courierId);
    }

    private String redirectToPanel(long courierId) {
        return REDIRECT_PREFIX + Paths.COURIER + "/" + courierId + Paths.COURIER_HOME;
    }
}
