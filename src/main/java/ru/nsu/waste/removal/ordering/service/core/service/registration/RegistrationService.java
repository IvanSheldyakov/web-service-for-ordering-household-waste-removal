package ru.nsu.waste.removal.ordering.service.core.service.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.exception.QuizValidationException;
import ru.nsu.waste.removal.ordering.service.core.model.registrationquiz.ActiveRegistrationQuizData;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.service.address.AddressService;
import ru.nsu.waste.removal.ordering.service.core.service.address.mapper.AddressMapper;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.person.mapper.PersonInfoMapper;
import ru.nsu.waste.removal.ordering.service.core.service.registrationquiz.RegistrationQuizService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserProfileService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserTypeService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String INVALID_TIMEZONE_MESSAGE =
            "\u041d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b\u0439 \u0447\u0430\u0441\u043e\u0432\u043e\u0439 \u043f\u043e\u044f\u0441";

    private final PersonInfoService personInfoService;
    private final PersonInfoMapper personInfoMapper;
    private final AddressService addressService;
    private final AddressMapper addressMapper;
    private final UserInfoService userInfoService;
    private final EcoTaskService ecoTaskService;
    private final UserProfileService userProfileService;
    private final RegistrationQuizService registrationQuizService;
    private final UserTypeService userTypeService;

    @Transactional
    public UserRegistrationResult register(RegistrationForm form, QuizAnswerForm quizAnswerForm) {
        ZoneId zoneId = validateTimezone(form.getTimezone());

        ActiveRegistrationQuizData quizData = registrationQuizService.getActiveQuizData(quizAnswerForm.getQuizId());

        Map<Long, Long> answers = quizAnswerForm.getAnswers() == null ? Map.of() : quizAnswerForm.getAnswers();
        registrationQuizService.validateAnswers(answers);

        UserType userType = userTypeService.resolveUserType(quizData, answers);

        long personId = personInfoService.add(personInfoMapper.toPersonCreationData(form));
        long addressId = addressService.add(addressMapper.toAddressCreationData(form, zoneId));
        long userId = userInfoService.add(userType, addressId, personId);

        ecoTaskService.assignStarterTasks(userType, userId, zoneId);

        return userProfileService.getUserProfile(userId);
    }

    private ZoneId validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new QuizValidationException(INVALID_TIMEZONE_MESSAGE);
        }
    }
}
