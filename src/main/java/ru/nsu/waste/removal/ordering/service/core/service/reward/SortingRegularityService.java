package ru.nsu.waste.removal.ordering.service.core.service.reward;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderDoneEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserGreenSlotContext;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.SortingRegularityWindowRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventHandler;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

@Service
@Order(12)
@RequiredArgsConstructor
public class SortingRegularityService implements UserActionEventHandler {

    private static final long ZERO_POINTS_DIFFERENCE = 0L;
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_MISSED = "MISSED";
    private static final String ORDER_TYPE_SEPARATE = "SEPARATE";
    private static final String ORDER_STATUS_DONE = "DONE";

    private final SortingRegularityWindowRepository sortingRegularityWindowRepository;
    private final UserInfoRepository userInfoRepository;
    private final OrderInfoService orderInfoService;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.eventType() == UserActionEventType.ORDER_DONE;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {
        if (!isSeparateDoneOrder(event.content())) {
            return;
        }

        ZoneId userZoneId = resolveUserZoneId(event.userId());
        TimeWindow weeklyWindow = weeklyWindowByTimestamp(event.createdAt(), userZoneId);
        tryEmitRegularityEventIfWindowIsNew(
                event.userId(),
                weeklyWindow,
                STATUS_CONFIRMED,
                "EVENT_PIPELINE"
        );
    }

    @Transactional
    public void syncClosedWeeklyWindows() {
        for (UserGreenSlotContext context : userInfoRepository.findAllGreenSlotContexts()) {
            ZoneId userZoneId = resolveUserZoneId(context.timezone());
            TimeWindow previousClosedWindow = previousClosedWeeklyWindow(userZoneId);

            long separateDoneCount = orderInfoService.countDoneSeparateOrdersCompletedInPeriod(
                    context.userId(),
                    previousClosedWindow.start(),
                    previousClosedWindow.end()
            );

            if (separateDoneCount > 0L) {
                tryEmitRegularityEventIfWindowIsNew(
                        context.userId(),
                        previousClosedWindow,
                        STATUS_CONFIRMED,
                        "SCHEDULED_JOB"
                );
            } else {
                tryEmitRegularityEventIfWindowIsNew(
                        context.userId(),
                        previousClosedWindow,
                        STATUS_MISSED,
                        "SCHEDULED_JOB"
                );
            }
        }
    }

    private void tryEmitRegularityEventIfWindowIsNew(
            long userId,
            TimeWindow window,
            String status,
            String source
    ) {
        boolean inserted = sortingRegularityWindowRepository.addWindowIfAbsent(
                userId,
                window.start(),
                window.end(),
                status
        );
        if (!inserted) {
            return;
        }

        boolean success = STATUS_CONFIRMED.equals(status);
        UserActionEventType eventType = success
                ? UserActionEventType.SORTING_REGULARITY_CONFIRMED
                : UserActionEventType.SORTING_REGULARITY_MISSED;

        userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                userId,
                eventType.dbName(),
                toJson(Map.of(
                        "windowStart", window.start().toString(),
                        "windowEnd", window.end().toString(),
                        "status", status,
                        "source", source,
                        "success", success
                )),
                ZERO_POINTS_DIFFERENCE
        ));
    }

    private boolean isSeparateDoneOrder(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return false;
        }
        try {
            OrderDoneEventContent content = objectMapper.readValue(contentJson, OrderDoneEventContent.class);
            return ORDER_TYPE_SEPARATE.equalsIgnoreCase(content.type())
                    && ORDER_STATUS_DONE.equalsIgnoreCase(content.status());
        } catch (Exception ignored) {
            return false;
        }
    }

    private TimeWindow weeklyWindowByTimestamp(OffsetDateTime timestamp, ZoneId userZoneId) {
        ZonedDateTime zoned = ZonedDateTime.ofInstant(timestamp.toInstant(), userZoneId);
        ZonedDateTime windowStart = startOfWeek(zoned);
        ZonedDateTime windowEnd = windowStart.plusWeeks(1);
        return new TimeWindow(
                windowStart.toOffsetDateTime(),
                windowEnd.toOffsetDateTime()
        );
    }

    private TimeWindow previousClosedWeeklyWindow(ZoneId userZoneId) {
        ZonedDateTime now = ZonedDateTime.ofInstant(clock.instant(), userZoneId);
        ZonedDateTime currentWeekStart = startOfWeek(now);
        ZonedDateTime previousWeekStart = currentWeekStart.minusWeeks(1);

        return new TimeWindow(
                previousWeekStart.toOffsetDateTime(),
                currentWeekStart.toOffsetDateTime()
        );
    }

    private ZonedDateTime startOfWeek(ZonedDateTime value) {
        return value
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private ZoneId resolveUserZoneId(long userId) {
        return userInfoRepository.findGreenSlotContextByUserId(userId)
                .map(UserGreenSlotContext::timezone)
                .map(this::resolveUserZoneId)
                .orElse(ZoneOffset.UTC);
    }

    private ZoneId resolveUserZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize sorting regularity event content", exception);
        }
    }

    private record TimeWindow(
            OffsetDateTime start,
            OffsetDateTime end
    ) {
    }
}
