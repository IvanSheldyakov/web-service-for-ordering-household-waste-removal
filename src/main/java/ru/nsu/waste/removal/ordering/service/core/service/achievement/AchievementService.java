package ru.nsu.waste.removal.ordering.service.core.service.achievement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.achievement.Achievement;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.achievement.AchievementRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;

    public List<Achievement> findByUserType(UserType userType) {
        return achievementRepository.findByUserType(userType);
    }
}
