package ru.nsu.waste.removal.ordering.service.app.controller.registration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.support.SessionStatus;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.facade.RegistrationFacade;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationFacade registrationFacade;

    @InjectMocks
    private RegistrationController registrationController;

    @Test
    void submitQuiz_whenRegistrationNotReady_redirectsToRegistration() {
        RegistrationForm form = new RegistrationForm();
        QuizAnswerForm quizAnswerForm = new QuizAnswerForm();
        SessionStatus sessionStatus = mock(SessionStatus.class);
        when(registrationFacade.isRegistrationReadyForQuiz(form)).thenReturn(false);

        String view = registrationController.submitQuiz(form, quizAnswerForm, sessionStatus);

        assertEquals("redirect:/registration", view);
        verify(registrationFacade, never()).registerAndCompleteSession(any(), any(), any());
    }

    @Test
    void submitQuiz_whenSuccessful_redirectsToUserHome() {
        RegistrationForm form = new RegistrationForm();
        QuizAnswerForm quizAnswerForm = new QuizAnswerForm();
        SessionStatus sessionStatus = mock(SessionStatus.class);
        long userId = 321L;
        when(registrationFacade.isRegistrationReadyForQuiz(form)).thenReturn(true);
        when(registrationFacade.registerAndCompleteSession(form, quizAnswerForm, sessionStatus))
                .thenReturn(registrationResult(userId));

        String view = registrationController.submitQuiz(form, quizAnswerForm, sessionStatus);

        assertEquals("redirect:/user/321/home", view);
        verify(registrationFacade).registerAndCompleteSession(form, quizAnswerForm, sessionStatus);
    }

    private RegistrationResultViewModel registrationResult(long userId) {
        return new RegistrationResultViewModel(
                userId,
                "Explorer",
                new RegistrationResultViewModel.BalanceViewModel(1000L, 1000L),
                new RegistrationResultViewModel.MotivationBlockViewModel(
                        "Explorer",
                        null,
                        null,
                        "050000",
                        "Message"
                ),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
