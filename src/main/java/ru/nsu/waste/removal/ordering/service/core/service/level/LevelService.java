package ru.nsu.waste.removal.ordering.service.core.service.level;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.mapper.level.LevelParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.event.LevelUpEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.level.AchieverLevelTarget;
import ru.nsu.waste.removal.ordering.service.core.model.level.Level;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventHandler;
import ru.nsu.waste.removal.ordering.service.core.service.level.param.EmitMaxLevelReachedEventParams;
import ru.nsu.waste.removal.ordering.service.core.service.level.param.LevelUpContentParams;

import java.util.Optional;

@Service
@Order(40)
@RequiredArgsConstructor
public class LevelService implements UserActionEventHandler {

    private final AchieverProfileRepository achieverProfileRepository;
    private final LevelRepository levelRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserInfoRepository userInfoRepository;
    private final LevelParamsMapper levelParamsMapper;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.eventType() != UserActionEventType.LEVEL_UP
                && event.eventType() != UserActionEventType.ACHIEVEMENT_UNLOCKED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {
        long positiveDelta = resolvePositiveDelta(event);
        if (positiveDelta == 0L) {
            return;
        }

        long newTotalPoints = userInfoRepository.findTotalPointsByUserId(event.userId()).orElse(0L);
        long oldTotalPoints = Math.max(0L, newTotalPoints - positiveDelta);
        updateLevelIfNeeded(event.userId(), oldTotalPoints, newTotalPoints);
    }

    private long resolvePositiveDelta(UserActionHistoryEvent event) {
        long pointsDifference = event.pointsDifference();
        if (pointsDifference == 0L) {
            pointsDifference = userActionHistoryRepository.findPointsDifferenceByEventId(event.id());
        }
        return Math.max(0L, pointsDifference);
    }

    private void updateLevelIfNeeded(long userId, long oldTotalPoints, long newTotalPoints) {
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
            userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                    userId,
                    UserActionEventType.LEVEL_UP.dbName(),
                    buildContentJson(levelParamsMapper.mapToLevelUpContentParams(
                            current,
                            desired,
                            oldTotalPoints,
                            newTotalPoints,
                            false
                    )),
                    0
            ));
            return;
        }

        emitMaxLevelReachedEventIfNeeded(
                levelParamsMapper.mapToEmitMaxLevelReachedEventParams(userId, current, oldTotalPoints, newTotalPoints)
        );
    }

    private void emitMaxLevelReachedEventIfNeeded(EmitMaxLevelReachedEventParams params) {
        Level highest = levelRepository.findHighestLevel();
        boolean isHighestTarget = params.current().id() == highest.id();
        boolean crossedMaxThreshold = params.oldTotalPoints() < params.current().requiredTotalPoints()
                && params.newTotalPoints() >= params.current().requiredTotalPoints();

        if (isHighestTarget && crossedMaxThreshold) {
            userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                    params.userId(),
                    UserActionEventType.LEVEL_UP.dbName(),
                    buildContentJson(levelParamsMapper.mapToLevelUpContentParams(
                            params.current(),
                            params.current(),
                            params.oldTotalPoints(),
                            params.newTotalPoints(),
                            true
                    )),
                    0
            ));
        }
    }

    private String buildContentJson(LevelUpContentParams params) {
        LevelUpEventContent content = new LevelUpEventContent(
                params.from().id(),
                params.to().id(),
                params.from().requiredTotalPoints(),
                params.to().requiredTotalPoints(),
                params.oldTotalPoints(),
                params.newTotalPoints(),
                params.maxReached()
        );

        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize level up content", exception);
        }
    }
}
