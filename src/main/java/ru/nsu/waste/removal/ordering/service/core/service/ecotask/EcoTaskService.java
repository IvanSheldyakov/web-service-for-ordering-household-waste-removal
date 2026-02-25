package ru.nsu.waste.removal.ordering.service.core.service.ecotask;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.AssignedEcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTask;
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskPeriod;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.EcoTaskRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.ecotask.UserEcoTaskRepository;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EcoTaskService {

    private static final int STARTER_TASK_LIMIT = 3;

    private final EcoTaskRepository ecoTaskRepository;
    private final UserEcoTaskRepository userEcoTaskRepository;
    private final Clock applicationClock;

    public void assignStarterTasks(UserType userType, long userId, ZoneId zoneId) {
        List<EcoTask> starterTasks = ecoTaskRepository.findActiveByUserType(userType.getId(), STARTER_TASK_LIMIT);
        for (EcoTask task : starterTasks) {
            if (userEcoTaskRepository.existsActiveAssignment(userId, task.id())) {
                continue;
            }
            OffsetDateTime expiredAt = calculateExpiredAt(task.period(), zoneId);
            userEcoTaskRepository.addAssigned(userId, task.id(), expiredAt);
        }
    }

    public List<AssignedEcoTask> findAssignedByUserId(long userId) {
        return userEcoTaskRepository.findAssignedByUserId(userId);
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
}
