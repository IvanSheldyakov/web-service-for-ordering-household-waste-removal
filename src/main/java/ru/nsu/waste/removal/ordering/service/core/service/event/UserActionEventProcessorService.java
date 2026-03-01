package ru.nsu.waste.removal.ordering.service.core.service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.repository.history.EventProcessorStateRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionEventProcessorService {

    private static final String PROCESSOR_NAME = "user-action-event-processor";
    private static final int BATCH_SIZE = 500;

    private final UserActionHistoryRepository userActionHistoryRepository;
    private final EventProcessorStateRepository eventProcessorStateRepository;
    private final List<UserActionEventHandler> eventHandlers;

    @Transactional
    public int processPendingEvents() {
        eventProcessorStateRepository.initProcessorStateIfAbsent(PROCESSOR_NAME);
        long lastProcessedEventId = eventProcessorStateRepository.findLastEventIdForUpdate(PROCESSOR_NAME);
        List<UserActionHistoryEvent> events = userActionHistoryRepository.findEventsAfterId(lastProcessedEventId, BATCH_SIZE);
        if (events.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (UserActionHistoryEvent event : events) {
            try {
                for (UserActionEventHandler handler : eventHandlers) {
                    if (!handler.supports(event)) {
                        continue;
                    }
                    handler.handle(event);
                }
            } catch (Exception exception) {
                log.error("Failed to process user action event id={}, userId={}, type={}",
                        event.id(), event.userId(), event.eventType(), exception);
            } finally {
                eventProcessorStateRepository.updateLastEventId(PROCESSOR_NAME, event.id());
            }
            processed++;
        }

        return processed;
    }
}
