package ru.nsu.waste.removal.ordering.service.core.service.level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.level.AchieverLevelTarget;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.model.event.LevelUpEventContent;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LevelService {

    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final ObjectMapper objectMapper;

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
