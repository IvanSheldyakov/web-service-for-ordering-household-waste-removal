package ru.nsu.waste.removal.ordering.service.app.controller.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.form.OrderCreateForm;
import ru.nsu.waste.removal.ordering.service.core.facade.OrderCreateFacade;

@Controller
@RequiredArgsConstructor
public class OrderCreateController {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final OrderCreateFacade orderCreateFacade;

    @GetMapping(Paths.USER_ORDER_CREATE)
    public String getOrderCreate(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.ORDER_CREATE_FORM, new OrderCreateForm());
        enrichModelForForm(userId, model);
        return TemplateNames.ORDER_CREATE;
    }

    @PostMapping(Paths.USER_ORDER_CREATE)
    public String createOrder(
            @PathVariable(Paths.USER_ID) long userId,
            @Valid @ModelAttribute(AttributeNames.ORDER_CREATE_FORM) OrderCreateForm form,
            BindingResult bindingResult,
            Model model
    ) {
        orderCreateFacade.validate(form, bindingResult, userId);
        if (bindingResult.hasErrors()) {
            enrichModelForForm(userId, model);
            return TemplateNames.ORDER_CREATE;
        }

        try {
            orderCreateFacade.createOrder(userId, form);
        } catch (IllegalStateException exception) {
            bindingResult.reject("orderCreate", exception.getMessage());
            enrichModelForForm(userId, model);
            return TemplateNames.ORDER_CREATE;
        }

        return REDIRECT_PREFIX + "/user/" + userId + "/home";
    }

    private void enrichModelForForm(long userId, Model model) {
        model.addAttribute(AttributeNames.USER_ID, userId);
        model.addAttribute(AttributeNames.ACTIVE_FRACTIONS, orderCreateFacade.getActiveFractions());
        model.addAttribute(AttributeNames.SLOT_OPTIONS, orderCreateFacade.getSlotOptions(userId));
        model.addAttribute(AttributeNames.CURRENT_POINTS, orderCreateFacade.getCurrentPoints(userId));
        model.addAttribute(AttributeNames.FIXED_COST_POINTS, orderCreateFacade.getFixedCostPoints());
        model.addAttribute(AttributeNames.ENOUGH_POINTS_FOR_PAYMENT, orderCreateFacade.hasEnoughPointsForPayment(userId));
    }
}
