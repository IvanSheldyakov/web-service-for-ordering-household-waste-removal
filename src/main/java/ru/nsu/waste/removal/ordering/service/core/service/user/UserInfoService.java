package ru.nsu.waste.removal.ordering.service.core.service.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserTypeInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserTypeRepository;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final UserTypeRepository userTypeRepository;
    private final LevelRepository levelRepository;

    @Transactional
    public long add(int userTypeId, long addressId, long personId) {
        long userId = userInfoRepository.addUserInfo(userTypeId, addressId, personId);

        Map<UserType, Integer> typeByName = userTypeRepository.findAll().stream()
                .collect(Collectors.toMap(UserTypeInfo::userType, UserTypeInfo::id));
        Integer achieverTypeId = typeByName.get(UserType.ACHIEVER);
        if (achieverTypeId != null && achieverTypeId == userTypeId) {
            Level firstLevel = levelRepository.findLowestLevel();
            userInfoRepository.addAchieverProfile(userId, firstLevel.id());
        }

        return userId;
    }
}

