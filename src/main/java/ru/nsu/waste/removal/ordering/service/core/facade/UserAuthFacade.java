package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import ru.nsu.waste.removal.ordering.service.app.form.UserLoginForm;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

@Service
@RequiredArgsConstructor
public class UserAuthFacade {

    private static final String PHONE_FIELD = "phone";
    private static final String USER_NOT_FOUND = "Пользователь с таким телефоном не найден";

    private final UserInfoService userInfoService;

    public Long login(UserLoginForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD)) {
            return null;
        }

        try {
            return userInfoService.findUserIdByPhone(form.getPhone());
        } catch (IllegalStateException | NumberFormatException exception) {
            bindingResult.rejectValue(PHONE_FIELD, "user.phone.notFound", USER_NOT_FOUND);
            return null;
        }
    }
}
