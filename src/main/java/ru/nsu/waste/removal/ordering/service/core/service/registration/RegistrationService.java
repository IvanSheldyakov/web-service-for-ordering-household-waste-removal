package ru.nsu.waste.removal.ordering.service.core.service.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.service.address.AddressService;
import ru.nsu.waste.removal.ordering.service.core.service.address.mapper.AddressMapper;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.person.mapper.PersonInfoMapper;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserPageService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserTypeService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final PersonInfoService personInfoService;
    private final PersonInfoMapper personInfoMapper;
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserInfoService userInfoService;
    private final EcoTaskService ecoTaskService;
    private final UserPageService userPageService;
    private final RegistrationQuizService registrationQuizService;
    private final UserTypeService userTypeService;

    @Transactional
    public RegistrationResultViewModel register(RegistrationForm form, QuizAnswerForm quizAnswerForm) {
        ZoneId zoneId = validateTimezone(form.getTimezone());

        ActiveRegistrationQuizData quizData = registrationQuizService.getActiveQuizData(quizAnswerForm.getQuizId());

        Map<Long, Long> answers = quizAnswerForm.getAnswers() == null ? Map.of() : quizAnswerForm.getAnswers();
        registrationQuizService.validateAnswers(answers);

        UserType userType = userTypeService.resolveUserType(quizData, answers);

        long personId = personInfoService.add(personInfoMapper.toPersonCreationData(form));
        long addressId = addressService.add(addressMapper.toAddressCreationData(form, zoneId));
        long userId = userInfoService.add(userType, addressId, personId);

        ecoTaskService.assignStarterTasks(userType, userId, zoneId);

        return userPageService.getUserPage(userId);
    }

    private ZoneId validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new QuizValidationException("Некорректный часовой пояс");
        }
    }
}
