package ru.nsu.waste.removal.ordering.service.core.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserGreenSlotContext;
import ru.nsu.waste.removal.ordering.service.core.repository.order.GreenSlotRepository;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GreenSlotService {

    private static final int SLOT_DURATION_HOURS = 2;
    private static final int HOURS_IN_DAY = 24;
    private static final int DAYS_VISIBLE = 2;

    private final UserInfoService userInfoService;
    private final GreenSlotRepository greenSlotRepository;
    private final Clock clock;

    public List<GreenSlot> getAvailableGreenSlots(long userId) {
        UserGreenSlotContext userContext = userInfoService.getGreenSlotContextByUserId(userId);
        ZoneId userZoneId = resolveZoneId(userContext);
        ZonedDateTime nowInUserZone = ZonedDateTime.now(clock).withZoneSameInstant(userZoneId);

        List<GreenSlot> availableSlots = buildAvailableSlots(nowInUserZone, userZoneId);
        if (availableSlots.isEmpty()) {
            return List.of();
        }

        OffsetDateTime periodFrom = nowInUserZone.toLocalDate().atStartOfDay(userZoneId).toOffsetDateTime();
        OffsetDateTime periodTo = nowInUserZone.toLocalDate().plusDays(DAYS_VISIBLE)
                .atStartOfDay(userZoneId)
                .toOffsetDateTime();

        Set<SlotKey> plannedSlotKeys = greenSlotRepository.findPlannedSlotsInPeriod(
                        userId,
                        userContext.postalCode(),
                        periodFrom,
                        periodTo
                ).stream()
                .map(SlotKey::fromSlot)
                .collect(Collectors.toSet());

        return availableSlots.stream()
                .filter(slot -> plannedSlotKeys.contains(SlotKey.fromSlot(slot)))
                .toList();
    }

    private ZoneId resolveZoneId(UserGreenSlotContext userContext) {
        try {
            return ZoneId.of(userContext.timezone());
        } catch (DateTimeException exception) {
            throw new IllegalStateException(
                    "User timezone is invalid for user id = %s".formatted(userContext.userId()),
                    exception
            );
        }
    }

    private List<GreenSlot> buildAvailableSlots(ZonedDateTime now, ZoneId zoneId) {
        LocalDate today = now.toLocalDate();
        List<GreenSlot> slots = new ArrayList<>();

        for (int dayOffset = 0; dayOffset < DAYS_VISIBLE; dayOffset++) {
            LocalDate day = today.plusDays(dayOffset);
            ZonedDateTime dayStart = day.atStartOfDay(zoneId);

            for (int hour = 0; hour < HOURS_IN_DAY; hour += SLOT_DURATION_HOURS) {
                ZonedDateTime slotStart = dayStart.plusHours(hour);
                if (!slotStart.isAfter(now)) {
                    continue;
                }
                ZonedDateTime slotEnd = slotStart.plusHours(SLOT_DURATION_HOURS);
                slots.add(new GreenSlot(
                        slotStart.toOffsetDateTime(),
                        slotEnd.toOffsetDateTime()
                ));
            }
        }

        return slots;
    }

    private record SlotKey(
            Instant pickupFrom,
            Instant pickupTo
    ) {

        private static SlotKey fromSlot(GreenSlot slot) {
            return new SlotKey(
                    slot.pickupFrom().toInstant(),
                    slot.pickupTo().toInstant()
            );
        }
    }
}
