package ru.nsu.waste.removal.ordering.service.core.service.reward;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value = "app.jobs.sorting-regularity.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SortingRegularityJob {

    private final SortingRegularityService sortingRegularityService;

    @Scheduled(
            fixedDelayString = "${app.jobs.sorting-regularity.fixed-delay-ms:300000}",
            initialDelayString = "${app.jobs.sorting-regularity.initial-delay-ms:300000}"
    )
    public void syncClosedWindows() {
        sortingRegularityService.syncClosedWeeklyWindows();
    }
}
