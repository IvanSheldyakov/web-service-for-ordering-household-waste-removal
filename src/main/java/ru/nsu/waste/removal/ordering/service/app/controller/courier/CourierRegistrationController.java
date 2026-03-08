package ru.nsu.waste.removal.ordering.service.app.controller.courier;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierRegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.view.CourierRegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierRegistrationFacade;

@Controller
@RequestMapping(Paths.COURIER_REGISTRATION)
@RequiredArgsConstructor
public class CourierRegistrationController {

    private final CourierRegistrationFacade courierRegistrationFacade;

    @ModelAttribute(AttributeNames.COURIER_REGISTRATION_FORM)
    public CourierRegistrationForm courierRegistrationForm() {
        return new CourierRegistrationForm();
    }

    @GetMapping
    public String getRegistrationForm(
            @ModelAttribute(AttributeNames.COURIER_REGISTRATION_FORM) CourierRegistrationForm form,
            Model model
    ) {
        model.addAttribute(AttributeNames.TIMEZONES, courierRegistrationFacade.getAvailableTimezones());
        return TemplateNames.COURIER_REGISTRATION;
    }

    @PostMapping
    public String submitRegistrationForm(
            @Valid @ModelAttribute(AttributeNames.COURIER_REGISTRATION_FORM) CourierRegistrationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        courierRegistrationFacade.validateRegistrationForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute(AttributeNames.TIMEZONES, courierRegistrationFacade.getAvailableTimezones());
            return TemplateNames.COURIER_REGISTRATION;
        }

        CourierRegistrationResultViewModel result = courierRegistrationFacade.register(form);
        model.addAttribute(AttributeNames.RESULT, result);
        return TemplateNames.COURIER_REGISTER_SUCCESS;
    }
}
