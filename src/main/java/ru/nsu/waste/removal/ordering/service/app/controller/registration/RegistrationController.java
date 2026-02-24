package ru.nsu.waste.removal.ordering.service.app.controller.registration;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationQuizViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.facade.RegistrationFacade;

@Controller
@RequestMapping(Paths.REGISTRATION)
@SessionAttributes(AttributeNames.REGISTRATION_FORM)
@RequiredArgsConstructor
public class RegistrationController {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final RegistrationFacade registrationFacade;

    @ModelAttribute(AttributeNames.REGISTRATION_FORM)
    public RegistrationForm registrationForm() {
        return new RegistrationForm();
    }

    @GetMapping
    public String getRegistrationForm(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            Model model
    ) {
        model.addAttribute(AttributeNames.TIMEZONES, registrationFacade.getAvailableTimezones());
        return TemplateNames.REGISTRATION_FORM;
    }

    @PostMapping
    public String submitRegistrationForm(
            @Valid @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        registrationFacade.validateRegistrationForm(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute(AttributeNames.TIMEZONES, registrationFacade.getAvailableTimezones());
            return TemplateNames.REGISTRATION_FORM;
        }
        return redirect(Paths.REGISTRATION_QUIZ);
    }

    @GetMapping(Paths.QUIZ)
    public String getQuiz(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            Model model
    ) {
        if (!registrationFacade.isRegistrationReadyForQuiz(form)) {
            return redirect(Paths.REGISTRATION);
        }
        RegistrationQuizViewModel quizViewModel = registrationFacade.getActiveQuizView();
        QuizAnswerForm quizAnswerForm = registrationFacade.createQuizAnswerForm(quizViewModel.quizId());

        model.addAttribute(AttributeNames.QUIZ, quizViewModel);
        model.addAttribute(AttributeNames.QUIZ_ANSWER_FORM, quizAnswerForm);
        return TemplateNames.REGISTRATION_QUIZ;
    }

    @PostMapping(Paths.QUIZ)
    public String submitQuiz(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            @ModelAttribute(AttributeNames.QUIZ_ANSWER_FORM) QuizAnswerForm quizAnswerForm,
            Model model,
            SessionStatus sessionStatus
    ) {
        if (!registrationFacade.isRegistrationReadyForQuiz(form)) {
            return redirect(Paths.REGISTRATION);
        }
        RegistrationResultViewModel result =
                registrationFacade.registerAndCompleteSession(form, quizAnswerForm, sessionStatus);
        model.addAttribute(AttributeNames.RESULT, result);
        return TemplateNames.REGISTER_SUCCESS;
    }

    private String redirect(String path) {
        return REDIRECT_PREFIX + path;
    }
}
