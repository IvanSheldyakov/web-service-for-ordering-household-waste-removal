package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierLoginForm;
import ru.nsu.waste.removal.ordering.service.app.form.CourierRegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.view.CourierRegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.TimezoneOptionViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierRegistrationService;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.timezone.TimezoneService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourierRegistrationFacade {

    private static final String PHONE_FIELD = "phone";
    private static final String PASSWORD_FIELD = "password";
    private static final String TIMEZONE_FIELD = "timezone";

    private static final String PHONE_ALREADY_REGISTERED = "Телефон уже зарегистрирован";
    private static final String TIMEZONE_INVALID = "Выберите часовой пояс из списка";
    private static final String COURIER_NOT_FOUND = "Курьер с таким телефоном не найден";
    private static final String INVALID_PASSWORD = "Неверный пароль";

    private final CourierRegistrationService courierRegistrationService;
    private final CourierInfoService courierInfoService;
    private final TimezoneService timezoneService;
    private final PersonInfoService personInfoService;

    public List<TimezoneOptionViewModel> getAvailableTimezones() {
        return timezoneService.getAvailableTimezoneOptions().stream()
                .map(option -> new TimezoneOptionViewModel(option.id(), option.title()))
                .toList();
    }

    public void validateRegistrationForm(CourierRegistrationForm form, BindingResult bindingResult) {
        validateTimezone(form, bindingResult);
        validatePhoneUniqueness(form, bindingResult);
    }

    public CourierRegistrationResultViewModel register(CourierRegistrationForm form) {
        CourierRegistrationResult result = courierRegistrationService.register(form);
        return new CourierRegistrationResultViewModel(
                result.courierId(),
                result.fullName(),
                result.postalCode(),
                result.totalPoints()
        );
    }

    public Long login(CourierLoginForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD) || bindingResult.hasFieldErrors(PASSWORD_FIELD)) {
            return null;
        }

        long courierId;
        try {
            courierId = courierInfoService.findCourierIdByPhone(form.getPhone());
        } catch (IllegalStateException | NumberFormatException exception) {
            bindingResult.rejectValue(PHONE_FIELD, "courier.phone.notFound", COURIER_NOT_FOUND);
            return null;
        }

        boolean passwordValid;
        try {
            passwordValid = personInfoService.isPasswordValid(form.getPhone(), form.getPassword());
        } catch (NumberFormatException exception) {
            bindingResult.rejectValue(PHONE_FIELD, "courier.phone.notFound", COURIER_NOT_FOUND);
            return null;
        }

        if (!passwordValid) {
            bindingResult.rejectValue(PASSWORD_FIELD, "courier.password.invalid", INVALID_PASSWORD);
            return null;
        }

        return courierId;
    }

    private void validateTimezone(CourierRegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(TIMEZONE_FIELD)) {
            return;
        }

        if (!timezoneService.isSupported(trim(form.getTimezone()))) {
            bindingResult.addError(new FieldError(
                    AttributeNames.COURIER_REGISTRATION_FORM,
                    TIMEZONE_FIELD,
                    TIMEZONE_INVALID
            ));
        }
    }

    private void validatePhoneUniqueness(CourierRegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD)) {
            return;
        }

        boolean exists;
        try {
            exists = personInfoService.isPhoneRegistered(trim(form.getPhone()));
        } catch (NumberFormatException exception) {
            return;
        }

        if (exists) {
            bindingResult.addError(new FieldError(
                    AttributeNames.COURIER_REGISTRATION_FORM,
                    PHONE_FIELD,
                    PHONE_ALREADY_REGISTERED
            ));
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
