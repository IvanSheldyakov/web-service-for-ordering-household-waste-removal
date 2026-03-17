package ru.nsu.waste.removal.ordering.service.app.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.form.UserLoginForm;
import ru.nsu.waste.removal.ordering.service.core.facade.UserAuthFacade;

@Controller
@RequestMapping(Paths.USER_LOGIN)
@RequiredArgsConstructor
public class UserAuthController {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final UserAuthFacade userAuthFacade;
    private final SecurityContextRepository securityContextRepository;

    @ModelAttribute(AttributeNames.USER_LOGIN_FORM)
    public UserLoginForm userLoginForm() {
        return new UserLoginForm();
    }

    @GetMapping
    public String getLoginForm() {
        return TemplateNames.USER_LOGIN;
    }

    @PostMapping
    public String login(
            @Valid @ModelAttribute(AttributeNames.USER_LOGIN_FORM) UserLoginForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            return TemplateNames.USER_LOGIN;
        }

        Long userId = userAuthFacade.login(form, bindingResult);
        if (userId == null) {
            return TemplateNames.USER_LOGIN;
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                form.getPhone().trim(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_USER")
        ));
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        return REDIRECT_PREFIX + Paths.USER + "/" + userId + Paths.HOME;
    }
}
