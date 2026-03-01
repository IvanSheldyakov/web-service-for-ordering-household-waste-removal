package ru.nsu.waste.removal.ordering.service.app.view;

import java.time.OffsetDateTime;
import java.util.List;

public record UserHomeViewModel(
        long userId,
        long totalPoints,
        long currentPoints,
        String userType,
        MotivationViewModel motivation,
        List<ActiveOrderViewModel> activeOrders
) {

    public record MotivationViewModel(
            AchieverMotivationViewModel achiever,
            SocializerMotivationViewModel socializer,
            ExplorerMotivationViewModel explorer
    ) {
    }

    public record AchieverMotivationViewModel(
            int levelId,
            Long nextRequiredPoints,
            long remaining,
            int progressPercent,
            boolean maxLevelReached
    ) {
    }

    public record SocializerMotivationViewModel(
            Integer weeklyRankPosition
    ) {
    }

    public record ExplorerMotivationViewModel(
            List<InfoCardViewModel> cards
    ) {
    }

    public record InfoCardViewModel(
            long id,
            String title,
            String description
    ) {
    }

    public record ActiveOrderViewModel(
            long orderId,
            String type,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            List<String> fractions
    ) {
    }
}

