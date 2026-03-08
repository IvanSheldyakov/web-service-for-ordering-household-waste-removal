package ru.nsu.waste.removal.ordering.service.core.service.courier;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.courier.CourierRepository;

@Service
@RequiredArgsConstructor
public class CourierInfoService {

    private static final String COURIER_NOT_FOUND_MESSAGE = "Курьер с таким телефоном не найден";

    private final CourierRepository courierRepository;

    public CourierProfileInfo getProfile(long courierId) {
        return courierRepository.findProfileById(courierId)
                .orElseThrow(() -> new IllegalStateException(
                        "Курьер с id = %s не найден".formatted(courierId)
                ));
    }

    public long findCourierIdByPhone(String phone) {
        long normalizedPhone = Long.parseLong(phone.trim());
        return courierRepository.findCourierIdByPhone(normalizedPhone)
                .orElseThrow(() -> new IllegalStateException(COURIER_NOT_FOUND_MESSAGE));
    }
}
