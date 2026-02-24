package ru.nsu.waste.removal.ordering.service.core.service.person;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.core.repository.person.PersonInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.model.person.PersonCreationData;

@Service
@RequiredArgsConstructor
public class PersonInfoService {

    private static final String PHONE_ALREADY_REGISTERED = "Телефон уже зарегистрирован";

    private final PersonInfoRepository personInfoRepository;

    public long add(PersonCreationData personCreationData) {
        try {
            return personInfoRepository.add(personCreationData);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatePhoneException(PHONE_ALREADY_REGISTERED, e);
        }
    }

    public boolean isPhoneRegistered(String phone) {
        return personInfoRepository.existsByPhone(Long.parseLong(phone));
    }
}
