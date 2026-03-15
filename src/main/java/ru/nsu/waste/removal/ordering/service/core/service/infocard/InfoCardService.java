package ru.nsu.waste.removal.ordering.service.core.service.infocard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.event.InfoCardViewedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.infocard.InfoCard;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.infocard.InfoCardRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InfoCardService {

    private static final int EXPLORER_CARD_LIMIT = 3;
    private static final long ZERO_POINTS_DIFFERENCE = 0L;

    private final InfoCardRepository infoCardRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final ObjectMapper objectMapper;

    public List<InfoCard> findByUserType(UserType userType) {
        return userType == UserType.EXPLORER
                ? infoCardRepository.findRandom(EXPLORER_CARD_LIMIT)
                : List.of();
    }

    public InfoCard openCard(long userId, long cardId) {
        InfoCard card = infoCardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalStateException("Info card with id = %s is not found".formatted(cardId)));

        userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                userId,
                UserActionEventType.INFO_CARD_VIEWED.dbName(),
                buildViewedContentJson(card),
                ZERO_POINTS_DIFFERENCE
        ));

        return card;
    }

    private String buildViewedContentJson(InfoCard card) {
        InfoCardViewedEventContent content = new InfoCardViewedEventContent(card.id(), card.title());
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize info card viewed content", exception);
        }
    }
}
