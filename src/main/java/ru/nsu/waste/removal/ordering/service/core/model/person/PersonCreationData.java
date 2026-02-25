package ru.nsu.waste.removal.ordering.service.core.model.person;

public record PersonCreationData(
        long phone,
        String email,
        String name,
        String surname,
        String patronymic
) {
}

