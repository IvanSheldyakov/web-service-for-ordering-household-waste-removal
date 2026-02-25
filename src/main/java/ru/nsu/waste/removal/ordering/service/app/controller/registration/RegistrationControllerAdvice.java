package ru.nsu.waste.removal.ordering.service.app.controller.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.constant.Paths;
import ru.nsu.waste.removal.ordering.service.app.constant.TemplateNames;
import ru.nsu.waste.removal.ordering.service.core.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.core.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationQuizViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.core.facade.RegistrationFacade;

@ControllerAdvice(assignableTypes = RegistrationController.class)
@RequiredArgsConstructor
public class RegistrationControllerAdvice {

    private static final String REDIRECT_PREFIX = "redirect:";

    private final RegistrationFacade registrationFacade;

    @ExceptionHandler(DuplicatePhoneException.class)
    public String handleDuplicatePhone(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute(
                AttributeNames.PHONE_CONFLICT_MESSAGE,
                registrationFacade.getPhoneAlreadyRegisteredMessage()
        );
        return REDIRECT_PREFIX + Paths.REGISTRATION;
    }

    @ExceptionHandler(QuizValidationException.class)
    public String handleQuizValidation(
            QuizValidationException exception,
            @ModelAttribute(AttributeNames.QUIZ_ANSWER_FORM) QuizAnswerForm quizAnswerForm,
            Model model
    ) {
        RegistrationQuizViewModel quiz = registrationFacade.getActiveQuizView();
        model.addAttribute(AttributeNames.QUIZ, quiz);
        model.addAttribute(AttributeNames.QUIZ_ANSWER_FORM, quizAnswerForm);
        model.addAttribute(AttributeNames.QUIZ_ERROR, exception.getMessage());
        return TemplateNames.REGISTRATION_QUIZ;
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(IllegalStateException exception, Model model) {
        model.addAttribute(AttributeNames.MESSAGE, exception.getMessage());
        return TemplateNames.REGISTER_QUIZ_UNAVAILABLE;
    }
}
