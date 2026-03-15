package ru.nsu.waste.removal.ordering.service.app.view;

public record UserInfoCardViewModel(
        long userId,
        long cardId,
        String title,
        String description
) {
}
