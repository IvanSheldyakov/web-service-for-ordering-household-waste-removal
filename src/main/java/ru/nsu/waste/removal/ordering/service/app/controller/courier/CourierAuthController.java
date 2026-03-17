package ru.nsu.waste.removal.ordering.service.app.controller.courier;

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
import ru.nsu.waste.removal.ordering.service.app.form.CourierLoginForm;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierRegistrationFacade;

@Controller
@RequestMapping(Paths.COURIER_LOGIN)
@RequiredArgsConstructor
public class CourierAuthController {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final CourierRegistrationFacade courierRegistrationFacade;
    private final SecurityContextRepository securityContextRepository;

    @ModelAttribute(AttributeNames.COURIER_LOGIN_FORM)
    public CourierLoginForm courierLoginForm() {
        return new CourierLoginForm();
    }

    @GetMapping
    public String getLoginForm() {
        return TemplateNames.COURIER_LOGIN;
    }

    @PostMapping
    public String login(
            @Valid @ModelAttribute(AttributeNames.COURIER_LOGIN_FORM) CourierLoginForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            return TemplateNames.COURIER_LOGIN;
        }

        Long courierId = courierRegistrationFacade.login(form, bindingResult);
        if (courierId == null) {
            return TemplateNames.COURIER_LOGIN;
        }

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                form.getPhone().trim(),
                null,
                AuthorityUtils.createAuthorityList("ROLE_COURIER")
        ));
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);

        return REDIRECT_PREFIX + Paths.COURIER + "/" + courierId + Paths.COURIER_HOME;
    }
}
