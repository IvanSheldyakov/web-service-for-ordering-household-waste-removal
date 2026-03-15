package ru.nsu.waste.removal.ordering.service.core.service.reward;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.AddEventParams;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class SortingRegularityE2ETest {

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
    private SortingRegularityService sortingRegularityService;

    @Autowired
    private UserActionHistoryRepository userActionHistoryRepository;

    @Autowired
    private UserActionEventProcessorService userActionEventProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
                    event_processor_state,
                    sorting_regularity_window,
                    achievement_user,
                    user_action_history,
                    order_waste_fraction,
                    order_info,
                    user_eco_task,
                    achiever_profile,
                    user_info,
                    address,
                    person_info
                restart identity cascade
                """);
    }

    @Test
    void processPendingEvents_whenSeparateOrderDoneInSameWeek_emitsConfirmedOnlyOnce() {
        long userId = createSocializer("79001110001", "630010", 1000L, 1000L, 0L);
        String content = """
                {"orderId":1,"type":"SEPARATE","greenChosen":false,"pickupFrom":"2026-03-19T10:00:00Z","pickupTo":"2026-03-19T12:00:00Z","fractionIds":[],"courierId":1,"status":"DONE"}
                """.trim();

        userActionHistoryRepository.addEvent(new AddEventParams(
                userId,
                UserActionEventType.ORDER_DONE.dbName(),
                content,
                0
        ));
        userActionEventProcessorService.processPendingEvents();

        userActionHistoryRepository.addEvent(new AddEventParams(
                userId,
                UserActionEventType.ORDER_DONE.dbName(),
                content,
                0
        ));
        processUntilNoPendingEvents();

        assertEquals(1, countEventsByType(userId, UserActionEventType.SORTING_REGULARITY_CONFIRMED.dbName()));
        assertEquals(1, countRegularityWindows(userId));
    }

    @Test
    void syncClosedWeeklyWindows_whenNoSeparateOrders_emitsMissedAndAppliesDecayIdempotently() {
        long userId = createSocializer("79001110002", "630011", 200L, 120L, 500_000L);

        sortingRegularityService.syncClosedWeeklyWindows();
        sortingRegularityService.syncClosedWeeklyWindows();
        processUntilNoPendingEvents();

        assertEquals(1, countEventsByType(userId, UserActionEventType.SORTING_REGULARITY_MISSED.dbName()));
        assertEquals(1, countRegularityWindows(userId));

        long missedDelta = findLatestPointsDifferenceByType(userId, UserActionEventType.SORTING_REGULARITY_MISSED.dbName());
        long habitStrengthAfter = findHabitStrength(userId);
        assertTrue(missedDelta <= 0L);
        assertTrue(habitStrengthAfter < 500_000L);
    }

    private int processUntilNoPendingEvents() {
        int totalProcessed = 0;
        for (int i = 0; i < 10; i++) {
            int processed = userActionEventProcessorService.processPendingEvents();
            totalProcessed += processed;
            if (processed == 0) {
                break;
            }
        }
        return totalProcessed;
    }

    private int countRegularityWindows(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from sorting_regularity_window where user_id = ?",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private int countEventsByType(long userId, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_action_history where user_id = ? and event_type = ?",
                Integer.class,
                userId,
                eventType
        );
        return count == null ? 0 : count;
    }

    private long findLatestPointsDifferenceByType(long userId, String eventType) {
        Long value = jdbcTemplate.queryForObject(
                """
                        select points_difference
                        from user_action_history
                        where user_id = ?
                          and event_type = ?
                        order by id desc
                        limit 1
                        """,
                Long.class,
                userId,
                eventType
        );
        return value == null ? 0L : value;
    }

    private long findHabitStrength(long userId) {
        Long value = jdbcTemplate.queryForObject(
                "select habit_strength from user_info where id = ?",
                Long.class,
                userId
        );
        return value == null ? 0L : value;
    }

    private long createSocializer(
            String phone,
            String postalCode,
            long totalPoints,
            long currentPoints,
            long habitStrength
    ) {
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
                "UTC"
        );

        Long userId = jdbcTemplate.queryForObject(
                """
                        insert into user_info(type_id, address_id, person_id, total_points, current_points, habit_strength)
                        values ((select id from user_type where name = 'SOCIALIZER'), ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                addressId,
                personId,
                totalPoints,
                currentPoints,
                habitStrength
        );

        if (personId == null || addressId == null || userId == null) {
            throw new IllegalStateException("Failed to prepare test user");
        }
        return userId;
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
