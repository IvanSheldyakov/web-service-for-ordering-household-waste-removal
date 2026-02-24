package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
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

    public UserProfileInfo getProfileByUserId(long userId) {
        return userInfoRepository.findProfileByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "\u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c \u0441 id = %s \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d"
                                .formatted(userId)
                ));
    }
}
