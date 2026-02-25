package ru.nsu.waste.removal.ordering.service.core.model.address;

public record AddressCreationData(
        String countryCode,
        String region,
        String city,
        String postalCode,
        String detailedAddress,
        String timezone
) {
}

