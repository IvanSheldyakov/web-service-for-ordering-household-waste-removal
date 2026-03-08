package ru.nsu.waste.removal.ordering.service.core.service.courier;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.app.form.CourierRegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.CourierRepository;
import ru.nsu.waste.removal.ordering.service.core.service.person.PersonInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.person.mapper.PersonInfoMapper;
import ru.nsu.waste.removal.ordering.service.core.service.timezone.TimezoneService;

@Service
@RequiredArgsConstructor
public class CourierRegistrationService {

    private static final String INVALID_TIMEZONE_MESSAGE = "Некорректный часовой пояс";

    private final PersonInfoService personInfoService;
    private final PersonInfoMapper personInfoMapper;
    private final CourierRepository courierRepository;
    private final TimezoneService timezoneService;

    @Transactional
    public CourierRegistrationResult register(CourierRegistrationForm form) {
        String timezone = normalizeTimezone(form.getTimezone());
        validateTimezone(timezone);

        long personId = personInfoService.add(personInfoMapper.toPersonCreationData(form));
        long courierId = courierRepository.add(
                personId,
                trim(form.getPostalCode()),
                timezone
        );

        CourierProfileInfo profile = courierRepository.findProfileById(courierId)
                .orElseThrow(() -> new IllegalStateException(
                        "Курьер с id = %s не найден".formatted(courierId)
                ));

        return new CourierRegistrationResult(
                profile.courierId(),
                profile.fullName(),
                profile.postalCode(),
                profile.totalPoints()
        );
    }

    private void validateTimezone(String timezone) {
        if (!timezoneService.isSupported(timezone)) {
            throw new IllegalStateException(INVALID_TIMEZONE_MESSAGE);
        }
    }

    private String normalizeTimezone(String timezone) {
        return timezone == null ? null : timezone.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
