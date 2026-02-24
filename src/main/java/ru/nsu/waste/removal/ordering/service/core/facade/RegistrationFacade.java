package ru.nsu.waste.removal.ordering.service.core.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.SessionStatus;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationQuizViewModel;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.service.timezone.TimezoneService;

import java.util.List;

import static ru.nsu.waste.removal.ordering.service.app.util.Validator.isRegistrationFilled;

@Service
@RequiredArgsConstructor
public class RegistrationFacade {

    private static final String PHONE_ALREADY_REGISTERED =
            "\u0422\u0435\u043b\u0435\u0444\u043e\u043d \u0443\u0436\u0435 \u0437\u0430\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0438\u0440\u043e\u0432\u0430\u043d";
    private static final String TIMEZONE_INVALID =
            "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0447\u0430\u0441\u043e\u0432\u043e\u0439 \u043f\u043e\u044f\u0441 \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430";
    private static final String PHONE_FIELD = "phone";
    private static final String TIMEZONE_FIELD = "timezone";

    private final RegistrationService registrationService;
    private final RegistrationQuizService registrationQuizService;
    private final TimezoneService timezoneService;
    private final PersonInfoService personInfoService;

    public List<String> getAvailableTimezones() {
        return timezoneService.getAvailableTimezones();
    }

    public boolean isRegistrationReadyForQuiz(RegistrationForm form) {
        return isRegistrationFilled(form);
    }

    public void validateRegistrationForm(RegistrationForm form, BindingResult bindingResult) {
        validateTimezone(form, bindingResult);
        validatePhoneUniqueness(form, bindingResult);
    }

    public RegistrationQuizViewModel getActiveQuizView() {
        return registrationQuizService.getActiveQuizView();
    }

    public QuizAnswerForm createQuizAnswerForm(long quizId) {
        QuizAnswerForm quizAnswerForm = new QuizAnswerForm();
        quizAnswerForm.setQuizId(quizId);
        return quizAnswerForm;
    }

    public RegistrationResultViewModel register(RegistrationForm form, QuizAnswerForm quizAnswerForm) {
        return registrationService.register(form, quizAnswerForm);
    }

    public RegistrationResultViewModel registerAndCompleteSession(
            RegistrationForm form,
            QuizAnswerForm quizAnswerForm,
            SessionStatus sessionStatus
    ) {
        RegistrationResultViewModel result = register(form, quizAnswerForm);
        sessionStatus.setComplete();
        return result;
    }

    public String getPhoneAlreadyRegisteredMessage() {
        return PHONE_ALREADY_REGISTERED;
    }

    private void validatePhoneUniqueness(RegistrationForm form, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors(PHONE_FIELD)) {
            return;
        }
        boolean exists;
        try {
            exists = personInfoService.isPhoneRegistered(form.getPhone().trim());
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

    private void validateTimezone(RegistrationForm form, BindingResult bindingResult) {
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
}
