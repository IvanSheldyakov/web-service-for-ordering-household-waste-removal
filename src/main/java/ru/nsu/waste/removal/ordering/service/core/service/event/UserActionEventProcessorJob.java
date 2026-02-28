package ru.nsu.waste.removal.ordering.service.core.service.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "app.jobs.user-action-event-processor.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UserActionEventProcessorJob {

    private final UserActionEventProcessorService userActionEventProcessorService;

    @Scheduled(fixedDelayString = "${app.jobs.user-action-event-processor.fixed-delay-ms:1000}")
    public void processEvents() {
        userActionEventProcessorService.processPendingEvents();
    }
}
