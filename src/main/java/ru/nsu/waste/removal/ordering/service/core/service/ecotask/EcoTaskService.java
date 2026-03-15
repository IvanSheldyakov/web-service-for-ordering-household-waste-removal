package ru.nsu.waste.removal.ordering.service.core.service.ecotask;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.mapper.ecotask.EcoTaskParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.mapper.history.UserActionHistoryParamsMapper;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.ActiveEcoTaskAssignment;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskRuleType;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.UserEcoTaskAssignmentItem;
import ru.nsu.waste.removal.ordering.service.core.model.event.EcoTaskCompletedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.EcoTaskRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.UserEcoTaskRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Order(20)
@RequiredArgsConstructor
public class EcoTaskService implements UserActionEventHandler {

    private static final int STARTER_TASK_LIMIT = 3;

    private final EcoTaskRepository ecoTaskRepository;
    private final UserEcoTaskRepository userEcoTaskRepository;
    private final UserActionHistoryRepository userActionHistoryRepository;
    private final EcoTaskParamsMapper ecoTaskParamsMapper;
    private final UserActionHistoryParamsMapper userActionHistoryParamsMapper;
    private final OrderInfoService orderInfoService;
    private final UserInfoRepository userInfoRepository;
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
            userEcoTaskRepository.addAssigned(
                    ecoTaskParamsMapper.mapToAddAssignedParams(userId, task.id(), assignedAt, expiredAt)
            );
        }
    }

    public List<AssignedEcoTask> findAssignedByUserId(long userId) {
        syncLifecycleForUser(userId);
        return userEcoTaskRepository.findAssignedByUserId(userId);
    }

    public List<UserEcoTaskAssignmentItem> findAllAssignmentsByUserId(long userId) {
        syncLifecycleForUser(userId);
        return userEcoTaskRepository.findAllAssignmentsByUserId(userId);
    }

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.eventType() != UserActionEventType.ECO_TASK_COMPLETED;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {
        syncLifecycleForUser(event.userId());

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

            userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                    event.userId(),
                    UserActionEventType.ECO_TASK_COMPLETED.dbName(),
                    buildCompletedContentJson(assignment),
                    assignment.points()
            ));

            userActionHistoryRepository.addEvent(userActionHistoryParamsMapper.mapToAddEventParams(
                    event.userId(),
                    UserActionEventType.ECO_TASK_REWARD_REQUEST.dbName(),
                    buildRewardRequestContentJson(assignment),
                    0L
            ));
        }

        syncLifecycleForUser(event.userId());
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
                    userActionHistoryParamsMapper.mapToCountByUserIdAndEventTypeInPeriodParams(
                            userId,
                            resolveActionCountEventType(assignment, triggerEvent).dbName(),
                            assignment.assignedAt(),
                            assignment.expiredAt()
                    )
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

    private String buildRewardRequestContentJson(ActiveEcoTaskAssignment assignment) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "success", true,
                    "sourceEventType", UserActionEventType.ECO_TASK_COMPLETED.dbName(),
                    "ecoTaskId", assignment.ecoTaskId(),
                    "ecoTaskCode", assignment.ecoTaskCode(),
                    "basePoints", assignment.points()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize eco task reward request content", exception);
        }
    }

    private void syncLifecycleForUser(long userId) {
        OffsetDateTime now = OffsetDateTime.now(applicationClock);
        userEcoTaskRepository.expireOverdueAssignmentsByUserId(userId, now);
        replenishActiveAssignments(userId, now);
    }

    private void replenishActiveAssignments(long userId, OffsetDateTime now) {
        UserType userType = userInfoRepository.findUserTypeByUserId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "User with id = %s is not found".formatted(userId)
                ));
        List<EcoTask> availableTasks = ecoTaskRepository.findActiveByUserType(userType.getId());
        if (availableTasks.isEmpty()) {
            return;
        }

        int targetActiveCount = Math.min(STARTER_TASK_LIMIT, availableTasks.size());
        int activeCount = userEcoTaskRepository.countActiveAssignmentsByUserId(userId);
        int missingCount = targetActiveCount - activeCount;
        if (missingCount <= 0) {
            return;
        }

        Set<Integer> activeTaskIds = userEcoTaskRepository.findActiveEcoTaskIdsByUserId(userId);
        Set<Integer> allAssignedTaskIds = userEcoTaskRepository.findAllEcoTaskIdsByUserId(userId);
        ZoneId zoneId = resolveUserZoneId(userId);

        List<EcoTask> freshTasks = new ArrayList<>();
        List<EcoTask> repeatedTasks = new ArrayList<>();
        for (EcoTask task : availableTasks) {
            if (activeTaskIds.contains(task.id())) {
                continue;
            }
            if (allAssignedTaskIds.contains(task.id())) {
                repeatedTasks.add(task);
            } else {
                freshTasks.add(task);
            }
        }

        missingCount = assignTasks(userId, now, zoneId, freshTasks, activeTaskIds, missingCount);
        assignTasks(userId, now, zoneId, repeatedTasks, activeTaskIds, missingCount);
    }

    private int assignTasks(
            long userId,
            OffsetDateTime now,
            ZoneId zoneId,
            List<EcoTask> tasks,
            Set<Integer> activeTaskIds,
            int missingCount
    ) {
        int remaining = missingCount;
        for (EcoTask task : tasks) {
            if (remaining <= 0) {
                break;
            }
            if (activeTaskIds.contains(task.id())) {
                continue;
            }

            OffsetDateTime assignedAt = OffsetDateTime.ofInstant(now.toInstant(), zoneId);
            OffsetDateTime expiredAt = calculateExpiredAt(task.period(), zoneId);
            boolean inserted = userEcoTaskRepository.addAssigned(
                    ecoTaskParamsMapper.mapToAddAssignedParams(userId, task.id(), assignedAt, expiredAt)
            );
            if (!inserted) {
                continue;
            }

            activeTaskIds.add(task.id());
            remaining--;
        }

        return remaining;
    }

    private ZoneId resolveUserZoneId(long userId) {
        String timezone = userInfoRepository.findGreenSlotContextByUserId(userId)
                .map(context -> context.timezone())
                .orElse(ZoneOffset.UTC.getId());
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }
}
