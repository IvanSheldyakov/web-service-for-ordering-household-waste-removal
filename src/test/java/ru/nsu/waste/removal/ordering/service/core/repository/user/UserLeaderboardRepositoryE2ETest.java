package ru.nsu.waste.removal.ordering.service.core.repository.user;

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
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class UserLeaderboardRepositoryE2ETest {

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
    private UserLeaderboardRepository userLeaderboardRepository;

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
    void findWeeklyRankPosition_ignoresOrderPaidWithPointsSpendingEvents() {
        long firstUserId = createUser("70010000001", "630000");
        long secondUserId = createUser("70010000002", "630000");

        addEvent(firstUserId, UserActionEventType.SEPARATE_CHOSEN.dbName(), 50L, "2026-03-19T10:00:00+00:00");
        addEvent(firstUserId, UserActionEventType.ORDER_PAID_WITH_POINTS.dbName(), -100L, "2026-03-19T11:00:00+00:00");
        addEvent(secondUserId, UserActionEventType.GREEN_SLOT_CHOSEN.dbName(), 40L, "2026-03-19T10:30:00+00:00");

        int rank = userLeaderboardRepository.findWeeklyRankPosition(
                        firstUserId,
                        OffsetDateTime.parse("2026-03-13T00:00:00+00:00")
                )
                .orElseThrow(() -> new IllegalStateException("Rank is expected"));

        assertEquals(1, rank);
    }

    private long createUser(String phone, String postalCode) {
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
                        insert into user_info(type_id, address_id, person_id)
                        values ((select id from user_type where name = 'SOCIALIZER'), ?, ?)
                        returning id
                        """,
                Long.class,
                addressId,
                personId
        );

        if (personId == null || addressId == null || userId == null) {
            throw new IllegalStateException("Failed to prepare test user");
        }
        return userId;
    }

    private void addEvent(long userId, String eventType, long pointsDifference, String createdAt) {
        jdbcTemplate.update(
                """
                        insert into user_action_history(user_id, event_type, content, points_difference, created_at)
                        values (?, ?, cast('{}' as jsonb), ?, ?::timestamptz)
                        """,
                userId,
                eventType,
                pointsDifference,
                createdAt
        );
    }
}

