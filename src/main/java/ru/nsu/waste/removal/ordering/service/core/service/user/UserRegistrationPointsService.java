package ru.nsu.waste.removal.ordering.service.core.service.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationPointsService {

    private final long initialPoints;

    public UserRegistrationPointsService(@Value("${app.user.initial-points:1000}") long initialPoints) {
        if (initialPoints <= 0L) {
            throw new IllegalStateException("Initial user points must be positive");
        }
        this.initialPoints = initialPoints;
    }

    public long getInitialPoints() {
        return initialPoints;
    }
}
