package ru.nsu.waste.removal.ordering.service.core.service.person;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.core.model.person.PersonCreationData;
import ru.nsu.waste.removal.ordering.service.core.repository.person.PersonInfoRepository;

@Service
@RequiredArgsConstructor
public class PersonInfoService {

    private static final String PHONE_ALREADY_REGISTERED = "Телефон уже зарегистрирован";

    private final PersonInfoRepository personInfoRepository;
    private final PasswordEncoder passwordEncoder;

    public long add(PersonCreationData personCreationData) {
        try {
            String encodedPassword = passwordEncoder.encode(personCreationData.password());
            return personInfoRepository.add(new PersonCreationData(
                    personCreationData.phone(),
                    personCreationData.email(),
                    personCreationData.name(),
                    personCreationData.surname(),
                    personCreationData.patronymic(),
                    encodedPassword
            ));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicatePhoneException(PHONE_ALREADY_REGISTERED, exception);
        }
    }

    public boolean isPhoneRegistered(String phone) {
        return personInfoRepository.existsByPhone(Long.parseLong(phone.trim()));
    }

    public boolean isPasswordValid(String phone, String rawPassword) {
        long normalizedPhone = Long.parseLong(phone.trim());
        String normalizedPassword = rawPassword == null ? "" : rawPassword.trim();
        return personInfoRepository.findPasswordHashByPhone(normalizedPhone)
                .filter(passwordHash -> !passwordHash.isBlank())
                .map(passwordHash -> passwordEncoder.matches(normalizedPassword, passwordHash))
                .orElse(false);
    }
}
