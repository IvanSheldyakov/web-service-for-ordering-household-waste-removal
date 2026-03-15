package ru.nsu.waste.removal.ordering.service.core.service.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.user.LeaderboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserLeaderboard;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserLeaderboardEntry;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserLeaderboardRepository;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserLeaderboardService {

    private static final long ZERO_POINTS_DIFFERENCE = 0L;

    private final UserLeaderboardRepository userLeaderboardRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public UserLeaderboard getLeaderboard(long userId, LeaderboardPeriod period, int topLimit) {
        OffsetDateTime since = OffsetDateTime.now(clock).minusDays(period.lengthDays());
        List<UserLeaderboardEntry> topEntries =
                userLeaderboardRepository.findTopEntriesByUserDistrict(userId, since, topLimit);
        UserLeaderboardEntry currentUserEntry = userLeaderboardRepository.findRankEntry(userId, since)
                .orElse(null);

        addLeaderboardOpenedEvent(userId, period);

        return new UserLeaderboard(
                userId,
                period,
                topEntries,
                currentUserEntry,
                topLimit
        );
    }

    private void addLeaderboardOpenedEvent(long userId, LeaderboardPeriod period) {
        userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                userId,
                UserActionEventType.LEADERBOARD_OPENED.dbName(),
                toJson(Map.of("period", period.name())),
                ZERO_POINTS_DIFFERENCE
        ));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize leaderboard event content", exception);
        }
    }
}
