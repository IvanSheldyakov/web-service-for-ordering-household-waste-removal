package ru.nsu.waste.removal.ordering.service.core.service.person.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.person.PersonCreationData;

@Mapper(componentModel = "spring")
public interface PersonInfoMapper {

    @Mapping(target = "phone", source = "phone", qualifiedByName = "trimToLong")
    @Mapping(target = "email", source = "email", qualifiedByName = "trim")
    @Mapping(target = "name", source = "name", qualifiedByName = "trim")
    @Mapping(target = "surname", source = "surname", qualifiedByName = "trim")
    @Mapping(target = "patronymic", source = "patronymic", qualifiedByName = "trimToNull")
    PersonCreationData toPersonCreationData(RegistrationForm form);

    @Named("trimToLong")
    default long trimToLong(String phone) {
        return Long.parseLong(phone.trim());
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("trimToNull")
    default String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

