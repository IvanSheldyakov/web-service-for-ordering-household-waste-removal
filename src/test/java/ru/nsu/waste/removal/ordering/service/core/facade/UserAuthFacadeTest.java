package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import ru.nsu.waste.removal.ordering.service.app.form.UserLoginForm;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthFacadeTest {

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private PersonInfoService personInfoService;

    @InjectMocks
    private UserAuthFacade userAuthFacade;

    @Test
    void login_returnsUserId_whenPhoneAndPasswordValid() {
        UserLoginForm form = new UserLoginForm();
        form.setPhone("79001234567");
        form.setPassword("secret123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userLoginForm");

        when(userInfoService.findUserIdByPhone("79001234567")).thenReturn(15L);
        when(personInfoService.isPasswordValid("79001234567", "secret123")).thenReturn(true);

        Long userId = userAuthFacade.login(form, bindingResult);

        assertEquals(15L, userId);
        assertTrue(!bindingResult.hasErrors());
    }

    @Test
    void login_returnsNullAndAddsFieldError_whenUserNotFound() {
        UserLoginForm form = new UserLoginForm();
        form.setPhone("79001234567");
        form.setPassword("secret123");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userLoginForm");

        when(userInfoService.findUserIdByPhone("79001234567"))
                .thenThrow(new IllegalStateException("Пользователь не найден"));

        Long userId = userAuthFacade.login(form, bindingResult);

        assertNull(userId);
        assertTrue(bindingResult.hasFieldErrors("phone"));
    }

    @Test
    void login_returnsNullAndAddsFieldError_whenPasswordInvalid() {
        UserLoginForm form = new UserLoginForm();
        form.setPhone("79001234567");
        form.setPassword("wrong");
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "userLoginForm");

        when(userInfoService.findUserIdByPhone("79001234567")).thenReturn(15L);
        when(personInfoService.isPasswordValid("79001234567", "wrong")).thenReturn(false);

        Long userId = userAuthFacade.login(form, bindingResult);

        assertNull(userId);
        assertTrue(bindingResult.hasFieldErrors("password"));
    }
}
