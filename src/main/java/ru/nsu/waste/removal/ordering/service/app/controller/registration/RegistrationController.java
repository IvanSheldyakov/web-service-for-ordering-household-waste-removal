package ru.nsu.waste.removal.ordering.service.app.controller.registration;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.view.RegistrationQuizViewModel;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.service.timezone.TimezoneService;

import static ru.nsu.waste.removal.ordering.service.app.util.Validator.isRegistrationFilled;

@Controller
@RequestMapping(Paths.REGISTRATION)
@SessionAttributes(AttributeNames.REGISTRATION_FORM)
@RequiredArgsConstructor
public class RegistrationController {

    private static final String PHONE_ALREADY_REGISTERED = "Телефон уже зарегистрирован";
    private static final String TIMEZONE_INVALID = "Выберите часовой пояс из списка";
    private static final String REDIRECT_PREFIX = "redirect:";
    private static final String PHONE_FIELD = "phone";
    private static final String TIMEZONE_FIELD = "timezone";

    private final RegistrationService registrationService;
    private final RegistrationQuizService registrationQuizService;
    private final TimezoneService timezoneService;

    @ModelAttribute(AttributeNames.REGISTRATION_FORM)
    public RegistrationForm registrationForm() {
        return new RegistrationForm();
    }

    @GetMapping
    public String getRegistrationForm(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            Model model
    ) {
        model.addAttribute(AttributeNames.TIMEZONES, timezoneService.getAvailableTimezones());
        return TemplateNames.REGISTRATION_FORM;
    }

    @PostMapping
    public String submitRegistrationForm(
            @Valid @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            BindingResult bindingResult,
            Model model
    ) {
        validateTimezoneField(form, bindingResult);
        validatePhoneUniqueness(form, bindingResult);
        if (bindingResult.hasErrors()) {
            model.addAttribute(AttributeNames.TIMEZONES, timezoneService.getAvailableTimezones());
            return TemplateNames.REGISTRATION_FORM;
        }
        return redirect(Paths.REGISTRATION_QUIZ);
    }

    @GetMapping(Paths.QUIZ)
    public String getQuiz(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            Model model
    ) {
        if (!isRegistrationFilled(form)) {
            return redirect(Paths.REGISTRATION);
        }
        try {
            RegistrationQuizViewModel quizViewModel = registrationQuizService.getActiveQuizView();
            QuizAnswerForm quizAnswerForm = new QuizAnswerForm();
            quizAnswerForm.setQuizId(quizViewModel.quizId());

            model.addAttribute(AttributeNames.QUIZ, quizViewModel);
            model.addAttribute(AttributeNames.QUIZ_ANSWER_FORM, quizAnswerForm);
            return TemplateNames.REGISTRATION_QUIZ;
        } catch (IllegalStateException e) {
            model.addAttribute(AttributeNames.MESSAGE, e.getMessage());
            return TemplateNames.REGISTER_QUIZ_UNAVAILABLE;
        }
    }

    @PostMapping(Paths.QUIZ)
    public String submitQuiz(
            @ModelAttribute(AttributeNames.REGISTRATION_FORM) RegistrationForm form,
            @ModelAttribute(AttributeNames.QUIZ_ANSWER_FORM) QuizAnswerForm quizAnswerForm,
            Model model,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes
    ) {
        if (!isRegistrationFilled(form)) {
            return redirect(Paths.REGISTRATION);
        }
        try {
            RegistrationResultViewModel result = registrationService.register(form, quizAnswerForm);
            sessionStatus.setComplete();
            model.addAttribute(AttributeNames.RESULT, result);
            return TemplateNames.REGISTER_SUCCESS;
        } catch (DuplicatePhoneException e) {
            redirectAttributes.addFlashAttribute(AttributeNames.PHONE_CONFLICT_MESSAGE, PHONE_ALREADY_REGISTERED);
            return redirect(Paths.REGISTRATION);
        } catch (QuizValidationException e) {
            RegistrationQuizViewModel quiz = registrationQuizService.getActiveQuizView();
            model.addAttribute(AttributeNames.QUIZ, quiz);
            model.addAttribute(AttributeNames.QUIZ_ANSWER_FORM, quizAnswerForm);
            model.addAttribute(AttributeNames.QUIZ_ERROR, e.getMessage());
            return TemplateNames.REGISTRATION_QUIZ;
        } catch (IllegalStateException e) {
            model.addAttribute(AttributeNames.MESSAGE, e.getMessage());
            return TemplateNames.REGISTER_QUIZ_UNAVAILABLE;
        }
    }

    private void validatePhoneUniqueness(RegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD)) {
            return;
        }
        boolean exists;
        try {
            exists = registrationService.isPhoneRegistered(form.getPhone().trim());
        } catch (NumberFormatException e) {
            return;
        }
        if (exists) {
            bindingResult.addError(new FieldError(
                    AttributeNames.REGISTRATION_FORM,
                    PHONE_FIELD,
                    PHONE_ALREADY_REGISTERED
            ));
        }
    }

    private void validateTimezoneField(RegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(TIMEZONE_FIELD)) {
            return;
        }
        if (!timezoneService.isSupported(form.getTimezone())) {
            bindingResult.addError(new FieldError(
                    AttributeNames.REGISTRATION_FORM,
                    TIMEZONE_FIELD,
                    TIMEZONE_INVALID
            ));
        }
    }

    private String redirect(String path) {
        return REDIRECT_PREFIX + path;
    }
}
