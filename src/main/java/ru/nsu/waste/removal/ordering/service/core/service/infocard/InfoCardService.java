package ru.nsu.waste.removal.ordering.service.core.service.infocard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.infocard.InfoCardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InfoCardService {

    private static final int EXPLORER_CARD_LIMIT = 3;

    private final InfoCardRepository infoCardRepository;

    public List<InfoCard> findByUserType(UserType userType) {
        return userType == UserType.EXPLORER
                ? infoCardRepository.findRandom(EXPLORER_CARD_LIMIT)
                : List.of();
    }
}
