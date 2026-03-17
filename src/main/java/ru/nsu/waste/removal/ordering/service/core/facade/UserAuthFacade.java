package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import ru.nsu.waste.removal.ordering.service.app.form.UserLoginForm;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

@Service
@RequiredArgsConstructor
public class UserAuthFacade {

    private static final String PHONE_FIELD = "phone";
    private static final String PASSWORD_FIELD = "password";

    private static final String USER_NOT_FOUND = "Пользователь с таким телефоном не найден";
    private static final String INVALID_PASSWORD = "Неверный пароль";

    private final UserInfoService userInfoService;
    private final PersonInfoService personInfoService;

    public Long login(UserLoginForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD) || bindingResult.hasFieldErrors(PASSWORD_FIELD)) {
            return null;
        }

        long userId;
        try {
            userId = userInfoService.findUserIdByPhone(form.getPhone());
        } catch (IllegalStateException | NumberFormatException exception) {
            bindingResult.rejectValue(PHONE_FIELD, "user.phone.notFound", USER_NOT_FOUND);
            return null;
        }

        boolean passwordValid;
        try {
            passwordValid = personInfoService.isPasswordValid(form.getPhone(), form.getPassword());
        } catch (NumberFormatException exception) {
            bindingResult.rejectValue(PHONE_FIELD, "user.phone.notFound", USER_NOT_FOUND);
            return null;
        }

        if (!passwordValid) {
            bindingResult.rejectValue(PASSWORD_FIELD, "user.password.invalid", INVALID_PASSWORD);
            return null;
        }

        return userId;
    }
}
