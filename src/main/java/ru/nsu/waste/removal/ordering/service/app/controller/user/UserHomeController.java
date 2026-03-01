package ru.nsu.waste.removal.ordering.service.app.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserHomePageService;

@Controller
@RequiredArgsConstructor
public class UserHomeController {

    private final UserHomePageService userHomePageService;

    @GetMapping(Paths.USER_HOME)
    public String getUserHome(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.HOME, userHomePageService.getHome(userId));
        return TemplateNames.USER_HOME;
    }

    @GetMapping(Paths.USER_ECO_DASHBOARD)
    public String getEcoDashboard(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.ECO_DASHBOARD;
    }

    @GetMapping(Paths.USER_HISTORY)
    public String getUserHistory(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.USER_HISTORY;
    }

    @GetMapping(Paths.USER_ORDER_CREATE)
    public String getOrderCreate(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.ORDER_CREATE;
    }
}

