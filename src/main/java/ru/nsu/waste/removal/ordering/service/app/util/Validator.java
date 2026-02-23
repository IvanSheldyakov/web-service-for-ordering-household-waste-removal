package ru.nsu.waste.removal.ordering.service.app.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Validator {

    public static boolean isRegistrationFilled(RegistrationForm form) {
        return hasText(form.getPhone())
                && hasText(form.getEmail())
                && hasText(form.getName())
                && hasText(form.getSurname())
                && hasText(form.getCountryCode())
                && hasText(form.getRegion())
                && hasText(form.getCity())
                && hasText(form.getPostalCode())
                && hasText(form.getDetailedAddress())
                && hasText(form.getTimezone());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
