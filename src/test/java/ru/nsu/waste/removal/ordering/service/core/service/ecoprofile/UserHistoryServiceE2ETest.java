package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistory;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class UserHistoryServiceE2ETest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.3-alpine")
            .withDatabaseName("wros_test")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void overrideDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }

    @Autowired
    private UserHistoryService userHistoryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
                    event_processor_state,
                    order_waste_fraction,
                    order_info,
                    user_action_history,
                    user_eco_task,
                    achiever_profile,
                    user_info,
                    address,
                    person_info
                restart identity cascade
                """);
    }

    @Test
    void getUserHistory_includesOrderPaidWithPointsWithSpecificDescription() {
        long userId = createUser("75550000001", "100001", 500L, 180L, "Asia/Novosibirsk");
        long paperId = findFractionIdByType("PAPER");
        long glassId = findFractionIdByType("GLASS");

        addEvent(
                userId,
                UserActionEventType.ORDER_CREATED.dbName(),
                """
                        {"orderId":101,"type":"SEPARATE","pickupFrom":"2026-03-20T10:00:00+00:00","pickupTo":"2026-03-20T12:00:00+00:00","greenChosen":true,"fractionIds":[%d,%d]}
                        """.formatted(paperId, glassId).trim(),
                0L,
                OffsetDateTime.parse("2026-03-20T12:00:00+00:00")
        );
        addEvent(
                userId,
                UserActionEventType.ORDER_PAID_WITH_POINTS.dbName(),
                """
                        {"orderId":101,"spentPoints":100}
                        """.trim(),
                -100L,
                OffsetDateTime.parse("2026-03-20T11:30:00+00:00")
        );
        addEvent(
                userId,
                UserActionEventType.SEPARATE_CHOSEN.dbName(),
                "{\"success\":true}",
                12L,
                OffsetDateTime.parse("2026-03-20T11:00:00+00:00")
        );
        addEvent(
                userId,
                UserActionEventType.GREEN_SLOT_CHOSEN.dbName(),
                "{\"success\":true}",
                8L,
                OffsetDateTime.parse("2026-03-20T10:50:00+00:00")
        );
        addEvent(
                userId,
                UserActionEventType.ORDER_DONE.dbName(),
                "{}",
                -15L,
                OffsetDateTime.parse("2026-03-20T10:40:00+00:00")
        );

        UserHistory history = userHistoryService.getUserHistory(userId);

        assertEquals(userId, history.userId());
        assertEquals(180L, history.currentPoints());
        assertEquals(5, history.items().size());
        assertEquals(
                OffsetDateTime.parse("2026-03-20T19:00:00+07:00"),
                history.items().getFirst().occurredAt()
        );
        assertTrue(history.items().getFirst().description().contains("раздельный вывоз"));
        assertTrue(history.items().stream().anyMatch(
                item -> "Оплата заказа баллами (-100)".equals(item.description()) && item.pointsDelta() == -100L
        ));
        assertTrue(history.items().stream().anyMatch(
                item -> "Списание баллов".equals(item.description()) && item.pointsDelta() == -15L
        ));

        List<OffsetDateTime> occurredAtList = history.items().stream()
                .map(item -> item.occurredAt())
                .toList();
        List<OffsetDateTime> sortedOccurredAtList = occurredAtList.stream()
                .sorted(Comparator.reverseOrder())
                .toList();
        assertEquals(sortedOccurredAtList, occurredAtList);

        long running = history.currentPoints();
        for (var item : history.items()) {
            assertEquals(running, item.balanceAfter());
            running = running - item.pointsDelta();
        }
    }

    @Test
    void getUserHistory_whenInvalidContent_usesFallbackDescriptions() {
        long userId = createUser("75550000002", "100002", 100L, 50L, "UTC");

        addEvent(
                userId,
                UserActionEventType.ORDER_CREATED.dbName(),
                "\"unexpected\"",
                0L,
                OffsetDateTime.parse("2026-03-20T12:10:00+00:00")
        );
        addEvent(
                userId,
                UserActionEventType.ECO_TASK_COMPLETED.dbName(),
                "[]",
                7L,
                OffsetDateTime.parse("2026-03-20T12:00:00+00:00")
        );

        UserHistory history = userHistoryService.getUserHistory(userId, 10);

        assertEquals(2, history.items().size());
        assertEquals("Оформлен заказ", history.items().get(0).description());
        assertEquals("Выполнено эко-задание", history.items().get(1).description());
    }

    private long createUser(String phone, String postalCode, long totalPoints, long currentPoints, String timezone) {
        Long personId = jdbcTemplate.queryForObject(
                """
                        insert into person_info(phone, email, name, surname, patronymic)
                        values (?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                Long.parseLong(phone),
                phone + "@mail.test",
                "Name",
                "Surname",
                null
        );

        Long addressId = jdbcTemplate.queryForObject(
                """
                        insert into address(country_code, postal_code, city, region, detailed_address, timezone)
                        values (?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                "RU",
                postalCode,
                "City",
                "Region",
                "Street 1",
                timezone
        );

        Long userId = jdbcTemplate.queryForObject(
                """
                        insert into user_info(type_id, address_id, person_id, total_points, current_points, habit_strength)
                        values ((select id from user_type where name = 'ACHIEVER'), ?, ?, ?, ?, 0)
                        returning id
                        """,
                Long.class,
                addressId,
                personId,
                totalPoints,
                currentPoints
        );

        if (personId == null || addressId == null || userId == null) {
            throw new IllegalStateException("Failed to prepare test user");
        }

        return userId;
    }

    private long findFractionIdByType(String type) {
        Long fractionId = jdbcTemplate.queryForObject(
                "select id from waste_fraction where type = ?",
                Long.class,
                type
        );
        if (fractionId == null) {
            throw new IllegalStateException("Failed to resolve fraction id for type " + type);
        }
        return fractionId;
    }

    private void addEvent(
            long userId,
            String eventType,
            String content,
            long pointsDifference,
            OffsetDateTime createdAt
    ) {
        Integer updated = jdbcTemplate.update(
                """
                        insert into user_action_history(user_id, event_type, content, points_difference, created_at)
                        values (?, ?, cast(? as jsonb), ?, ?)
                        """,
                userId,
                eventType,
                content,
                pointsDifference,
                createdAt
        );
        if (updated == null || updated != 1) {
            throw new IllegalStateException("Failed to insert history event");
        }
    }
}

