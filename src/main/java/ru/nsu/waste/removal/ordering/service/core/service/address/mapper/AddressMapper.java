package ru.nsu.waste.removal.ordering.service.core.service.address.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.address.AddressCreationData;

import java.time.ZoneId;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "countryCode", source = "form.countryCode", qualifiedByName = "trimAndUpper")
    @Mapping(target = "region", source = "form.region", qualifiedByName = "trim")
    @Mapping(target = "city", source = "form.city", qualifiedByName = "trim")
    @Mapping(target = "postalCode", source = "form.postalCode", qualifiedByName = "trim")
    @Mapping(target = "detailedAddress", source = "form.detailedAddress", qualifiedByName = "trim")
    @Mapping(target = "timezone", source = "zoneId", qualifiedByName = "zoneIdToString")
    AddressCreationData toAddressCreationData(RegistrationForm form, ZoneId zoneId);

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("trimAndUpper")
    default String trimAndUpper(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    @Named("zoneIdToString")
    default String zoneIdToString(ZoneId zoneId) {
        return zoneId.getId();
    }
}

