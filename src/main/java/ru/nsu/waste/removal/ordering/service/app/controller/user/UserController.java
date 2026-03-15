package ru.nsu.waste.removal.ordering.service.app.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.LeaderboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.facade.UserFacade;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserFacade userFacade;

    @GetMapping(Paths.USER_HOME)
    public String getUserHome(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.HOME, userFacade.getHome(userId));
        return TemplateNames.USER_HOME;
    }

    @GetMapping(Paths.USER_ECO_DASHBOARD)
    public String getEcoDashboard(
            @PathVariable(Paths.USER_ID) long userId,
            @RequestParam(name = "period", required = false) String period,
            Model model
    ) {
        EcoDashboardPeriod selectedPeriod = EcoDashboardPeriod.fromQuery(period);
        model.addAttribute(
                AttributeNames.DASHBOARD,
                userFacade.getDashboard(userId, selectedPeriod)
        );
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.ECO_DASHBOARD;
    }

    @GetMapping(Paths.USER_HISTORY)
    public String getUserHistory(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.HISTORY, userFacade.getHistory(userId));
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.USER_HISTORY;
    }

    @GetMapping(Paths.USER_LEADERBOARD)
    public String getUserLeaderboard(
            @PathVariable(Paths.USER_ID) long userId,
            @RequestParam(name = "period", required = false) String period,
            Model model
    ) {
        LeaderboardPeriod selectedPeriod = LeaderboardPeriod.fromQuery(period);
        model.addAttribute(AttributeNames.LEADERBOARD, userFacade.getLeaderboard(userId, selectedPeriod));
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.USER_LEADERBOARD;
    }

    @GetMapping(Paths.USER_INFO_CARD)
    public String getInfoCard(
            @PathVariable(Paths.USER_ID) long userId,
            @PathVariable(Paths.CARD_ID) long cardId,
            Model model
    ) {
        model.addAttribute(AttributeNames.INFO_CARD, userFacade.getInfoCard(userId, cardId));
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.USER_INFO_CARD;
    }

    @GetMapping(Paths.USER_GAMIFICATION)
    public String getGamification(@PathVariable(Paths.USER_ID) long userId, Model model) {
        model.addAttribute(AttributeNames.GAMIFICATION, userFacade.getGamification(userId));
        model.addAttribute(AttributeNames.USER_ID, userId);
        return TemplateNames.USER_GAMIFICATION;
    }
}
