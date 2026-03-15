package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistory;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistoryItem;
import ru.nsu.waste.removal.ordering.service.core.model.event.EcoTaskCompletedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.InfoCardViewedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.LiederRewardEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.OrderCreatedEventContent;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryRecord;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.order.WasteFractionRepository;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private static final int DEFAULT_LIMIT = 10;
    private static final String EVENT_TYPE_ORDER_CREATED = UserActionEventType.ORDER_CREATED.dbName();
    private static final String EVENT_TYPE_ORDER_PAID_WITH_POINTS = UserActionEventType.ORDER_PAID_WITH_POINTS.dbName();
    private static final String EVENT_TYPE_SEPARATE_CHOSEN = UserActionEventType.SEPARATE_CHOSEN.dbName();
    private static final String EVENT_TYPE_GREEN_SLOT_CHOSEN = UserActionEventType.GREEN_SLOT_CHOSEN.dbName();
    private static final String EVENT_TYPE_ECO_TASK_COMPLETED = UserActionEventType.ECO_TASK_COMPLETED.dbName();
    private static final String EVENT_TYPE_INFO_CARD_VIEWED = UserActionEventType.INFO_CARD_VIEWED.dbName();
    private static final String EVENT_TYPE_LEADERBOARD_OPENED = UserActionEventType.LEADERBOARD_OPENED.dbName();
    private static final String EVENT_TYPE_ECO_TASK_REWARD_REQUEST = UserActionEventType.ECO_TASK_REWARD_REQUEST.dbName();
    private static final String EVENT_TYPE_SORTING_REGULARITY_CONFIRMED = UserActionEventType.SORTING_REGULARITY_CONFIRMED.dbName();
    private static final String EVENT_TYPE_SORTING_REGULARITY_MISSED = UserActionEventType.SORTING_REGULARITY_MISSED.dbName();
    private static final String ORDER_TYPE_SEPARATE = "SEPARATE";

    private static final String ORDER_CREATED_FALLBACK = "Оформлен заказ";
    private static final String ORDER_PAID_WITH_POINTS_DESCRIPTION = "Оплата заказа баллами";
    private static final String SEPARATE_CHOSEN_DESCRIPTION = "Начисление за раздельный вывоз";
    private static final String GREEN_SLOT_CHOSEN_DESCRIPTION = "Начисление за выбор зелёного слота";
    private static final String ECO_TASK_COMPLETED_FALLBACK = "Выполнено эко-задание";
    private static final String INFO_CARD_VIEWED_FALLBACK = "Просмотрена информационная карточка";
    private static final String LEADERBOARD_OPENED_DESCRIPTION = "Открыта страница рейтинга";
    private static final String SORTING_REGULARITY_CONFIRMED_DESCRIPTION = "Подтверждена регулярность сортировки";
    private static final String SORTING_REGULARITY_MISSED_DESCRIPTION = "Пропущено окно регулярной сортировки";
    private static final String POINTS_WITHDRAW_DESCRIPTION = "Списание баллов";
    private static final String DEFAULT_ACTION_DESCRIPTION = "Действие пользователя";
    private static final String UNKNOWN_SLOT_TIME = "слот: —";
    private static final String UNKNOWN_FRACTIONS = "фракции: —";

    private static final DateTimeFormatter SLOT_FROM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SLOT_TO_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private static final Comparator<UserActionHistoryRecord> HISTORY_COMPARATOR =
            Comparator.comparing(UserActionHistoryRecord::createdAt, Comparator.reverseOrder())
                    .thenComparing(UserActionHistoryRecord::id, Comparator.reverseOrder());

    private static final List<String> INCLUDED_EVENT_TYPES = List.of(
            EVENT_TYPE_ORDER_CREATED,
            EVENT_TYPE_ORDER_PAID_WITH_POINTS,
            EVENT_TYPE_SEPARATE_CHOSEN,
            EVENT_TYPE_GREEN_SLOT_CHOSEN,
            EVENT_TYPE_ECO_TASK_COMPLETED,
            EVENT_TYPE_INFO_CARD_VIEWED,
            EVENT_TYPE_LEADERBOARD_OPENED,
            EVENT_TYPE_ECO_TASK_REWARD_REQUEST,
            EVENT_TYPE_SORTING_REGULARITY_CONFIRMED,
            EVENT_TYPE_SORTING_REGULARITY_MISSED
    );

    private final UserActionHistoryRepository userActionHistoryRepository;
    private final UserInfoService userInfoService;
    private final WasteFractionRepository wasteFractionRepository;
    private final ObjectMapper objectMapper;

    public UserHistory getUserHistory(long userId) {
        return getUserHistory(userId, DEFAULT_LIMIT);
    }

    public UserHistory getUserHistory(long userId, int limit) {
        int safeLimit = limit <= 0 ? DEFAULT_LIMIT : limit;
        long currentPoints = userInfoService.getProfileByUserId(userId).currentPoints();
        ZoneId userZoneId = resolveUserZoneId(userId);
        List<UserActionHistoryRecord> events = loadLatestEvents(userId, safeLimit);
        List<UserHistoryItem> items = buildItems(events, currentPoints, userZoneId);

        return new UserHistory(
                userId,
                currentPoints,
                items
        );
    }

    private List<UserActionHistoryRecord> loadLatestEvents(long userId, int limit) {
        List<UserActionHistoryRecord> eventsByType = userActionHistoryRepository.findLatestEventsByUserId(
                userId,
                INCLUDED_EVENT_TYPES,
                limit
        );
        List<UserActionHistoryRecord> negativeEvents = userActionHistoryRepository.findLatestNegativeEventsByUserId(
                userId,
                limit
        );

        return mergeAndLimit(eventsByType, negativeEvents, limit);
    }

    private List<UserActionHistoryRecord> mergeAndLimit(
            List<UserActionHistoryRecord> eventsByType,
            List<UserActionHistoryRecord> negativeEvents,
            int limit
    ) {
        Map<Long, UserActionHistoryRecord> uniqueEvents = new HashMap<>();
        for (UserActionHistoryRecord event : eventsByType) {
            uniqueEvents.putIfAbsent(event.id(), event);
        }
        for (UserActionHistoryRecord event : negativeEvents) {
            uniqueEvents.putIfAbsent(event.id(), event);
        }

        return uniqueEvents.values().stream()
                .sorted(HISTORY_COMPARATOR)
                .limit(limit)
                .toList();
    }

    private List<UserHistoryItem> buildItems(
            List<UserActionHistoryRecord> events,
            long currentPoints,
            ZoneId userZoneId
    ) {
        List<UserHistoryItem> items = new ArrayList<>(events.size());
        long running = currentPoints;

        for (UserActionHistoryRecord event : events) {
            long balanceAfter = running;
            items.add(new UserHistoryItem(
                    convertToUserTimezone(event.createdAt(), userZoneId),
                    buildDescription(event, userZoneId),
                    event.pointsDifference(),
                    balanceAfter
            ));
            running = running - event.pointsDifference();
        }

        return items;
    }

    private String buildDescription(UserActionHistoryRecord event, ZoneId userZoneId) {
        String eventType = event.eventType() == null
                ? ""
                : event.eventType().trim().toUpperCase(Locale.ROOT);

        if (EVENT_TYPE_ORDER_PAID_WITH_POINTS.equals(eventType)) {
            return buildOrderPaidWithPointsDescription(event.pointsDifference());
        }
        if (EVENT_TYPE_ORDER_CREATED.equals(eventType)) {
            return buildOrderCreatedDescription(event.content(), userZoneId);
        }
        if (EVENT_TYPE_SEPARATE_CHOSEN.equals(eventType)) {
            return SEPARATE_CHOSEN_DESCRIPTION;
        }
        if (EVENT_TYPE_GREEN_SLOT_CHOSEN.equals(eventType)) {
            return GREEN_SLOT_CHOSEN_DESCRIPTION;
        }
        if (EVENT_TYPE_ECO_TASK_COMPLETED.equals(eventType)) {
            return buildEcoTaskCompletedDescription(event.content());
        }
        if (EVENT_TYPE_INFO_CARD_VIEWED.equals(eventType)) {
            return buildInfoCardViewedDescription(event.content());
        }
        if (EVENT_TYPE_LEADERBOARD_OPENED.equals(eventType)) {
            return LEADERBOARD_OPENED_DESCRIPTION;
        }
        if (EVENT_TYPE_SORTING_REGULARITY_CONFIRMED.equals(eventType)) {
            return SORTING_REGULARITY_CONFIRMED_DESCRIPTION;
        }
        if (EVENT_TYPE_SORTING_REGULARITY_MISSED.equals(eventType)) {
            return SORTING_REGULARITY_MISSED_DESCRIPTION;
        }
        if (EVENT_TYPE_ECO_TASK_REWARD_REQUEST.equals(eventType)) {
            return buildAdaptiveRewardDescription(event.content(), "Адаптивная награда за эко-задание");
        }
        if (event.pointsDifference() < 0L) {
            return POINTS_WITHDRAW_DESCRIPTION;
        }
        return DEFAULT_ACTION_DESCRIPTION;
    }

    private String buildOrderPaidWithPointsDescription(long pointsDifference) {
        if (pointsDifference < 0L) {
            return ORDER_PAID_WITH_POINTS_DESCRIPTION + " (" + pointsDifference + ")";
        }
        return ORDER_PAID_WITH_POINTS_DESCRIPTION;
    }

    private String buildOrderCreatedDescription(String contentJson, ZoneId userZoneId) {
        OrderCreatedEventContent content = tryRead(contentJson, OrderCreatedEventContent.class);
        if (content == null) {
            return ORDER_CREATED_FALLBACK;
        }

        String slotDescription = buildSlotDescription(content.pickupFrom(), content.pickupTo(), userZoneId);
        boolean separateOrder = ORDER_TYPE_SEPARATE.equalsIgnoreCase(content.type());
        String greenSlotSuffix = content.greenChosen() ? ", зелёный слот" : "";

        if (!separateOrder) {
            return "Оформлен заказ: смешанный вывоз, " + slotDescription + greenSlotSuffix;
        }

        String fractions = resolveFractionNames(content.fractionIds());
        return "Оформлен заказ: раздельный вывоз (" + fractions + "), " + slotDescription + greenSlotSuffix;
    }

    private String buildEcoTaskCompletedDescription(String contentJson) {
        EcoTaskCompletedEventContent content = tryRead(contentJson, EcoTaskCompletedEventContent.class);
        if (content == null) {
            return ECO_TASK_COMPLETED_FALLBACK;
        }

        return "Выполнено эко-задание (+" + content.rewardPoints() + ")";
    }

    private String buildInfoCardViewedDescription(String contentJson) {
        InfoCardViewedEventContent content = tryRead(contentJson, InfoCardViewedEventContent.class);
        if (content == null || content.title() == null || content.title().isBlank()) {
            return INFO_CARD_VIEWED_FALLBACK;
        }

        return "Просмотрена карточка: " + content.title();
    }

    private String buildAdaptiveRewardDescription(String contentJson, String prefix) {
        LiederRewardEventContent content = tryRead(contentJson, LiederRewardEventContent.class);
        if (content == null) {
            return prefix;
        }

        long applied = content.appliedPoints();
        if (applied > 0L) {
            return prefix + " (+" + applied + ")";
        }
        if (applied < 0L) {
            return prefix + " (" + applied + ")";
        }
        return prefix + " (0)";
    }

    private String resolveFractionNames(List<Long> fractionIds) {
        List<String> fractionNames = wasteFractionRepository.findActiveFractionsByIds(fractionIds).stream()
                .map(fraction -> fraction.name())
                .toList();

        if (fractionNames.isEmpty()) {
            return UNKNOWN_FRACTIONS;
        }
        return String.join(", ", fractionNames);
    }

    private String buildSlotDescription(String pickupFrom, String pickupTo, ZoneId userZoneId) {
        OffsetDateTime pickupFromDateTime = tryParseDateTime(pickupFrom);
        OffsetDateTime pickupToDateTime = tryParseDateTime(pickupTo);
        if (pickupFromDateTime == null || pickupToDateTime == null) {
            return UNKNOWN_SLOT_TIME;
        }

        return "слот: " + SLOT_FROM_FORMATTER.format(convertToUserTimezone(pickupFromDateTime, userZoneId))
                + "-"
                + SLOT_TO_FORMATTER.format(convertToUserTimezone(pickupToDateTime, userZoneId));
    }

    private ZoneId resolveUserZoneId(long userId) {
        String timezone = userInfoService.getGreenSlotContextByUserId(userId).timezone();
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }

    private OffsetDateTime convertToUserTimezone(OffsetDateTime value, ZoneId userZoneId) {
        if (value == null) {
            return null;
        }
        return value.atZoneSameInstant(userZoneId).toOffsetDateTime();
    }

    private OffsetDateTime tryParseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T tryRead(String contentJson, Class<T> targetClass) {
        if (contentJson == null || contentJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(contentJson, targetClass);
        } catch (Exception ignored) {
            return null;
        }
    }
}
