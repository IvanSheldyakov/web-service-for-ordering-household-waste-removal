package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final LevelRepository levelRepository;

    @Transactional
    public long add(UserType userType, long addressId, long personId) {
        long userId = userInfoRepository.addUserInfo(userType.getId(), addressId, personId);
        if (userType == UserType.ACHIEVER) {
            Level firstLevel = levelRepository.findLowestLevel();
            userInfoRepository.addAchieverProfile(userId, firstLevel.id());
        }

        return userId;
    }
}
