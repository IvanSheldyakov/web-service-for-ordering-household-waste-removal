package ru.nsu.waste.removal.ordering.service.core.service.ecotask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.ActiveEcoTaskAssignment;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskRuleType;
import ru.nsu.waste.removal.ordering.service.core.model.event.EcoTaskCompletedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.EcoTaskRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.UserEcoTaskRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventHandler;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@Order(20)
@RequiredArgsConstructor
public class EcoTaskService implements UserActionEventHandler {

    private static final int STARTER_TASK_LIMIT = 3;

    private final EcoTaskRepository ecoTaskRepository;
    private final UserEcoTaskRepository userEcoTaskRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final OrderInfoService orderInfoService;
    private final ObjectMapper objectMapper;
    private final Clock applicationClock;

    @Transactional
    public void assignStarterTasks(UserType userType, long userId, ZoneId zoneId) {
        List<EcoTask> starterTasks = ecoTaskRepository.findActiveByUserType(userType.getId(), STARTER_TASK_LIMIT);
        OffsetDateTime assignedAt = OffsetDateTime.ofInstant(applicationClock.instant(), zoneId);
        for (EcoTask task : starterTasks) {
            if (userEcoTaskRepository.existsActiveAssignment(userId, task.id())) {
                continue;
            }
            OffsetDateTime expiredAt = calculateExpiredAt(task.period(), zoneId);
            userEcoTaskRepository.addAssigned(userId, task.id(), assignedAt, expiredAt);
        }
    }

    public List<AssignedEcoTask> findAssignedByUserId(long userId) {
        return userEcoTaskRepository.findAssignedByUserId(userId);
    }

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.eventType() != UserActionEventType.ECO_TASK_COMPLETED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {

        List<ActiveEcoTaskAssignment> assignments =
                userEcoTaskRepository.findActiveAssignmentsByUserIdAndTriggerEvent(
                        event.userId(),
                        event.eventType(),
                        OffsetDateTime.now(applicationClock)
                );

        for (ActiveEcoTaskAssignment assignment : assignments) {
            if (!isConditionMet(event.userId(), event.eventType(), assignment)) {
                continue;
            }

            boolean markedDone = userEcoTaskRepository.markDone(assignment.userEcoTaskId());
            if (!markedDone) {
                continue;
            }

            userActionHistoryRepository.addEvent(
                    event.userId(),
                    UserActionEventType.ECO_TASK_COMPLETED.dbName(),
                    buildCompletedContentJson(assignment),
                    assignment.points()
            );
        }
    }

    private OffsetDateTime calculateExpiredAt(EcoTaskPeriod period, ZoneId zoneId) {
        ZonedDateTime now = ZonedDateTime.ofInstant(applicationClock.instant(), zoneId);
        ZonedDateTime zonedExpiration;
        if (period == EcoTaskPeriod.WEEKLY) {
            zonedExpiration = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(0);
            if (zonedExpiration.isBefore(now)) {
                zonedExpiration = zonedExpiration.plusWeeks(1);
            }
        } else {
            zonedExpiration = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(0);
            if (zonedExpiration.isBefore(now)) {
                zonedExpiration = now.plusMonths(1)
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .withHour(23)
                        .withMinute(59)
                        .withSecond(59)
                        .withNano(0);
            }
        }
        return OffsetDateTime.ofInstant(zonedExpiration.toInstant(), zoneId);
    }

    private boolean isConditionMet(long userId, UserActionEventType triggerEvent, ActiveEcoTaskAssignment assignment) {
        return switch (assignment.ruleType()) {
            case ORDER_COUNT -> orderInfoService.countOrdersByFiltersInPeriod(
                    buildOrderFiltersInPeriod(userId, assignment)
            ) >= assignment.target();
            case DISTINCT_FRACTIONS -> orderInfoService.countDistinctFractionsByFiltersInPeriod(
                    buildOrderFiltersInPeriod(userId, assignment)
            ) >= assignment.target();
            case ACTION_COUNT -> userActionHistoryRepository.countByUserIdAndEventTypeInPeriod(
                    userId,
                    resolveActionCountEventType(assignment, triggerEvent).dbName(),
                    assignment.assignedAt(),
                    assignment.expiredAt()
            ) >= assignment.target();
        };
    }

    private UserActionEventType resolveActionCountEventType(
            ActiveEcoTaskAssignment assignment,
            UserActionEventType triggerEvent
    ) {
        if (assignment.ruleType() != EcoTaskRuleType.ACTION_COUNT) {
            return triggerEvent;
        }
        return assignment.actionEventTypeFilter() == null ? triggerEvent : assignment.actionEventTypeFilter();
    }

    private OrderFiltersInPeriod buildOrderFiltersInPeriod(long userId, ActiveEcoTaskAssignment assignment) {
        return new OrderFiltersInPeriod(
                userId,
                assignment.assignedAt(),
                assignment.expiredAt(),
                assignment.orderStatusFilter(),
                assignment.orderTypeFilter(),
                assignment.greenChosenFilter()
        );
    }

    private String buildCompletedContentJson(ActiveEcoTaskAssignment assignment) {
        EcoTaskCompletedEventContent content = new EcoTaskCompletedEventContent(
                assignment.userEcoTaskId(),
                assignment.ecoTaskId(),
                assignment.ecoTaskCode(),
                assignment.points()
        );
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize eco task completion content", exception);
        }
    }
}
