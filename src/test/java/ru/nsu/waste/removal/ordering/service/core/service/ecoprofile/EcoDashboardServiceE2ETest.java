package ru.nsu.waste.removal.ordering.service.core.service.ecoprofile;

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
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboard;
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.EcoDashboardPeriod;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class EcoDashboardServiceE2ETest {

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
    private EcoDashboardService ecoDashboardService;

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
    void getDashboard_whenWeekPeriod_buildsStatsFractionsInsightsAchieverAndWritesEvent() {
        long userId = createUser("79000000001", "100001", "ACHIEVER", 1500L, 900L);
        jdbcTemplate.update(
                "insert into achiever_profile(user_id, level_id) values (?, ?)",
                userId,
                2
        );

        OrderRow currentSeparateOne = addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-03-14T10:00:00+00:00"),
                "100001"
        );
        OrderRow currentSeparateTwo = addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-03-16T10:00:00+00:00"),
                "100001"
        );
        addOrder(
                userId,
                "MIXED",
                "DONE",
                OffsetDateTime.parse("2026-03-18T10:00:00+00:00"),
                "100001"
        );

        addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-03-10T10:00:00+00:00"),
                "100001"
        );
        addOrder(
                userId,
                "MIXED",
                "DONE",
                OffsetDateTime.parse("2026-03-11T10:00:00+00:00"),
                "100001"
        );

        addOrder(
                userId,
                "SEPARATE",
                "NEW",
                OffsetDateTime.parse("2026-03-19T10:00:00+00:00"),
                "100001"
        );

        addOrderFraction(currentSeparateOne, "PAPER");
        addOrderFraction(currentSeparateOne, "PLASTIC");
        addOrderFraction(currentSeparateTwo, "GLASS");

        EcoDashboard dashboard = ecoDashboardService.getDashboard(userId, EcoDashboardPeriod.WEEK);

        assertEquals(userId, dashboard.userId());
        assertEquals("ACHIEVER", dashboard.userType());
        assertEquals(900L, dashboard.currentPoints());
        assertEquals(1500L, dashboard.totalPoints());

        assertEquals("WEEK", dashboard.period().key());
        assertEquals(3, dashboard.period().options().size());
        assertEquals(1L, dashboard.period().options().stream().filter(option -> option.selected()).count());

        assertEquals(3L, dashboard.orders().doneTotal());
        assertEquals(2L, dashboard.orders().doneSeparate());
        assertEquals(66, dashboard.orders().separateSharePercent());
        assertEquals(List.of(
                "\u0411\u0443\u043c\u0430\u0433\u0430",
                "\u041f\u043b\u0430\u0441\u0442\u0438\u043a",
                "\u0421\u0442\u0435\u043a\u043b\u043e"
        ), dashboard.fractions());

        assertFalse(dashboard.insights().isEmpty());
        assertTrue(dashboard.insights().getFirst().contains(
                "\u0417\u0430 \u043f\u0435\u0440\u0438\u043e\u0434 \u0440\u0430\u0437\u0434\u0435\u043b\u044c\u043d\u044b\u0445 \u0437\u0430\u043a\u0430\u0437\u043e\u0432: 2."
        ));
        assertTrue(dashboard.insights().getFirst().contains("+1"));

        assertNotNull(dashboard.achiever());
        assertEquals(2, dashboard.achiever().levelId());
        assertEquals(2000L, dashboard.achiever().nextRequiredPoints());
        assertEquals(500L, dashboard.achiever().remaining());
        assertEquals(75, dashboard.achiever().progressPercent());
        assertFalse(dashboard.achiever().maxLevelReached());

        Map<String, Object> event = findLatestEcoProfileOpenedEvent(userId);
        assertEquals("ECO_PROFILE_OPENED", event.get("event_type"));
        assertEquals("WEEK", event.get("period"));
        assertEquals(0L, asLong(event.get("points_difference")));
    }

    @Test
    void getDashboard_whenAllPeriod_usesAllTimeStatsAndWritesAllEvent() {
        long userId = createUser("79000000002", "100002", "EXPLORER", 300L, 120L);

        OrderRow oldSeparate = addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-02-12T11:00:00+00:00"),
                "100002"
        );
        addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-03-01T11:00:00+00:00"),
                "100002"
        );
        addOrder(
                userId,
                "MIXED",
                "DONE",
                OffsetDateTime.parse("2026-03-19T11:00:00+00:00"),
                "100002"
        );
        addOrder(
                userId,
                "MIXED",
                "ASSIGNED",
                OffsetDateTime.parse("2026-03-19T12:00:00+00:00"),
                "100002"
        );

        addOrderFraction(oldSeparate, "METAL");

        EcoDashboard dashboard = ecoDashboardService.getDashboard(userId, EcoDashboardPeriod.ALL);

        assertEquals("ALL", dashboard.period().key());
        assertEquals(3L, dashboard.orders().doneTotal());
        assertEquals(2L, dashboard.orders().doneSeparate());
        assertEquals(66, dashboard.orders().separateSharePercent());
        assertEquals(List.of("\u041c\u0435\u0442\u0430\u043b"), dashboard.fractions());
        assertFalse(dashboard.insights().isEmpty());
        assertNull(dashboard.achiever());

        Map<String, Object> event = findLatestEcoProfileOpenedEvent(userId);
        assertEquals("ALL", event.get("period"));
        assertEquals(0L, asLong(event.get("points_difference")));
    }

    private long createUser(
            String phone,
            String postalCode,
            String userType,
            long totalPoints,
            long currentPoints
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
                        values ((select id from user_type where name = ?), ?, ?, ?, ?, 0)
                        returning id
                        """,
                Long.class,
                userType,
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

    private OrderRow addOrder(
            long userId,
            String type,
            String status,
            OffsetDateTime createdAt,
            String postalCode
    ) {
        OrderRow order = jdbcTemplate.queryForObject(
                """
                        insert into order_info(
                            user_id,
                            created_at,
                            type,
                            status,
                            pickup_from,
                            pickup_to,
                            postal_code,
                            cost_points,
                            green_chosen
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id, created_at
                        """,
                (rs, rowNum) -> new OrderRow(
                        rs.getLong("id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                userId,
                createdAt,
                type,
                status,
                createdAt.plusHours(1),
                createdAt.plusHours(2),
                postalCode,
                100L,
                false
        );

        if (order == null) {
            throw new IllegalStateException("Failed to insert test order");
        }

        return order;
    }

    private void addOrderFraction(OrderRow orderRow, String fractionType) {
        Long fractionId = jdbcTemplate.queryForObject(
                "select id from waste_fraction where type = ?",
                Long.class,
                fractionType
        );

        if (fractionId == null) {
            throw new IllegalStateException("Failed to resolve fraction id");
        }

        jdbcTemplate.update(
                """
                        insert into order_waste_fraction(order_id, order_created_at, fraction_id)
                        values (?, ?, ?)
                        """,
                orderRow.id(),
                orderRow.createdAt(),
                fractionId
        );
    }

    private Map<String, Object> findLatestEcoProfileOpenedEvent(long userId) {
        return jdbcTemplate.queryForMap(
                """
                        select event_type,
                               points_difference,
                               content ->> 'period' as period
                        from user_action_history
                        where user_id = ?
                          and event_type = 'ECO_PROFILE_OPENED'
                        order by id desc
                        limit 1
                        """,
                userId
        );
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private record OrderRow(
            long id,
            OffsetDateTime createdAt
    ) {
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
