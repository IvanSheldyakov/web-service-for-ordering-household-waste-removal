package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.service.timezone.TimezoneService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationFacadeTest {

    @Mock
    private RegistrationService registrationService;

    @Mock
    private RegistrationQuizService registrationQuizService;

    @Mock
    private TimezoneService timezoneService;

    @Mock
    private PersonInfoService personInfoService;

    @InjectMocks
    private RegistrationFacade registrationFacade;

    @Test
    void register_convertsEcoTaskExpiredAtToUserTimezone() {
        RegistrationForm form = new RegistrationForm();
        form.setTimezone("Asia/Novosibirsk");
        QuizAnswerForm quizAnswerForm = new QuizAnswerForm();

        UserRegistrationResult serviceResult = new UserRegistrationResult(
                15L,
                "Исследователь",
                new UserRegistrationResult.Balance(120L, 80L),
                new UserRegistrationResult.MotivationBlock(
                        "Исследователь",
                        null,
                        null,
                        null,
                        "Текст"
                ),
                List.of(
                        new UserRegistrationResult.EcoTask(
                                101L,
                                "Задача",
                                "Описание",
                                25L,
                                OffsetDateTime.parse("2026-03-03T10:00:00+00:00")
                        )
                ),
                List.of(),
                List.of()
        );
        when(registrationService.register(form, quizAnswerForm)).thenReturn(serviceResult);

        RegistrationResultViewModel result = registrationFacade.register(form, quizAnswerForm);

        assertEquals(1, result.ecoTasks().size());
        assertEquals(
                OffsetDateTime.parse("2026-03-03T17:00:00+07:00"),
                result.ecoTasks().getFirst().expiredAt()
        );
    }
}
