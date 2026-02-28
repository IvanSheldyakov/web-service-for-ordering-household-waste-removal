package ru.nsu.waste.removal.ordering.service.core.service.level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.level.AchieverLevelTarget;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.event.LevelUpEventContent;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserInfoRepository userInfoRepository;
    private final ObjectMapper objectMapper;
    private final ThreadLocal<Map<Long, Long>> rollingTotalsByUser = new ThreadLocal<>();

    public void prepareBatch(List<UserActionHistoryEvent> events) {
        Map<Long, Long> positivePointsByUser = new HashMap<>();
        for (UserActionHistoryEvent event : events) {
            if (event.eventType() == UserActionEventType.LEVEL_UP
                    || event.eventType() == UserActionEventType.ACHIEVEMENT_UNLOCKED) {
                continue;
            }

            long positiveDelta = Math.max(0L, event.pointsDifference());
            if (positiveDelta == 0L) {
                continue;
            }
            positivePointsByUser.merge(event.userId(), positiveDelta, Long::sum);
        }

        if (positivePointsByUser.isEmpty()) {
            rollingTotalsByUser.set(Map.of());
            return;
        }

        Map<Long, Long> currentTotalsByUser = userInfoRepository.findTotalPointsByUserIds(
                List.copyOf(positivePointsByUser.keySet())
        );

        Map<Long, Long> initialRollingTotals = new HashMap<>();
        for (Map.Entry<Long, Long> entry : positivePointsByUser.entrySet()) {
            long userId = entry.getKey();
            long positiveSumInBatch = entry.getValue();
            long currentTotal = currentTotalsByUser.getOrDefault(userId, 0L);
            initialRollingTotals.put(userId, Math.max(0L, currentTotal - positiveSumInBatch));
        }
        rollingTotalsByUser.set(initialRollingTotals);
    }

    public void clearBatch() {
        rollingTotalsByUser.remove();
    }

    public void processUserAction(UserActionHistoryEvent event) {
        if (event.eventType() == UserActionEventType.LEVEL_UP
                || event.eventType() == UserActionEventType.ACHIEVEMENT_UNLOCKED) {
            return;
        }

        long positiveDelta = Math.max(0L, event.pointsDifference());
        if (positiveDelta == 0L) {
            return;
        }

        Map<Long, Long> rollingTotals = rollingTotalsByUser.get();
        if (rollingTotals == null) {
            rollingTotals = new HashMap<>();
            rollingTotalsByUser.set(rollingTotals);
        }

        long oldTotalPoints = rollingTotals.getOrDefault(event.userId(), 0L);
        long newTotalPoints = oldTotalPoints + positiveDelta;
        rollingTotals.put(event.userId(), newTotalPoints);
        updateLevelIfNeeded(event.userId(), oldTotalPoints, newTotalPoints);
    }

    @Transactional
    public void updateLevelIfNeeded(long userId, long oldTotalPoints, long newTotalPoints) {
        Optional<AchieverLevelTarget> currentTargetOpt = achieverProfileRepository.findLevelTargetForUpdate(userId);
        if (currentTargetOpt.isEmpty()) {
            return;
        }

        AchieverLevelTarget currentTarget = currentTargetOpt.get();
        Level current = new Level(currentTarget.levelId(), currentTarget.requiredTotalPoints());

        Level desired = levelRepository.findNextTargetLevelByTotalPoints(newTotalPoints)
                .orElseGet(levelRepository::findHighestLevel);

        if (desired.id() != current.id()) {
            if (desired.requiredTotalPoints() <= current.requiredTotalPoints()) {
                return;
            }

            achieverProfileRepository.updateLevel(userId, desired.id());
            userActionHistoryRepository.addEvent(
                    userId,
                    UserActionEventType.LEVEL_UP.dbName(),
                    buildContentJson(current, desired, oldTotalPoints, newTotalPoints, false),
                    0
            );
            return;
        }

        // Особый случай: пользователь достиг максимального порога, но "следующей цели" уже нет.
        // Чтобы не спамить событиями, эмитим LEVEL_UP только в момент пересечения порога.
        emitMaxLevelReachedEventIfNeeded(userId, current, oldTotalPoints, newTotalPoints);
    }

    private void emitMaxLevelReachedEventIfNeeded(
            long userId,
            Level current,
            long oldTotalPoints,
            long newTotalPoints
    ) {
        Level highest = levelRepository.findHighestLevel();
        boolean isHighestTarget = current.id() == highest.id();
        boolean crossedMaxThreshold = oldTotalPoints < current.requiredTotalPoints()
                && newTotalPoints >= current.requiredTotalPoints();

        if (isHighestTarget && crossedMaxThreshold) {
            userActionHistoryRepository.addEvent(
                    userId,
                    UserActionEventType.LEVEL_UP.dbName(),
                    buildContentJson(current, current, oldTotalPoints, newTotalPoints, true),
                    0
            );
        }
    }

    private String buildContentJson(
            Level from,
            Level to,
            long oldTotalPoints,
            long newTotalPoints,
            boolean maxReached
    ) {
        LevelUpEventContent content = new LevelUpEventContent(
                from.id(),
                to.id(),
                from.requiredTotalPoints(),
                to.requiredTotalPoints(),
                oldTotalPoints,
                newTotalPoints,
                maxReached
        );

        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize level up content", exception);
        }
    }

}
