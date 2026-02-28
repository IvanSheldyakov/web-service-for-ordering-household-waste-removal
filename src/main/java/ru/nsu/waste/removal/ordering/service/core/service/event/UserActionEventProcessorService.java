package ru.nsu.waste.removal.ordering.service.core.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.repository.history.EventProcessorStateRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.service.achievement.AchievementService;
import ru.nsu.waste.removal.ordering.service.core.service.level.LevelService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionEventProcessorService {

    private static final String PROCESSOR_NAME = "user-action-event-processor";
    private static final int BATCH_SIZE = 500;

    private final UserActionHistoryRepository userActionHistoryRepository;
    private final EventProcessorStateRepository eventProcessorStateRepository;
    private final AchievementService achievementService;
    private final LevelService levelService;

    @Transactional
    public int processPendingEvents() {
        eventProcessorStateRepository.initProcessorStateIfAbsent(PROCESSOR_NAME);
        long lastProcessedEventId = eventProcessorStateRepository.findLastEventIdForUpdate(PROCESSOR_NAME);
        List<UserActionHistoryEvent> events = userActionHistoryRepository.findEventsAfterId(lastProcessedEventId, BATCH_SIZE);
        if (events.isEmpty()) {
            return 0;
        }

        levelService.prepareBatch(events);
        int processed = 0;
        try {
            for (UserActionHistoryEvent event : events) {
                try {
                    achievementService.processUserAction(event.userId(), event.eventType());
                    levelService.processUserAction(event);
                } catch (Exception exception) {
                    log.error("Failed to process user action event id={}, userId={}, type={}",
                            event.id(), event.userId(), event.eventType(), exception);
                } finally {
                    eventProcessorStateRepository.updateLastEventId(PROCESSOR_NAME, event.id());
                }
                processed++;
            }
        } finally {
            levelService.clearBatch();
        }

        return processed;
    }
}
