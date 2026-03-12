package ru.nsu.waste.removal.ordering.service.core.repository.user.param;

public record AddUserInfoParams(
        int typeId,
        long addressId,
        long personId,
        long initialPoints
) {
}
