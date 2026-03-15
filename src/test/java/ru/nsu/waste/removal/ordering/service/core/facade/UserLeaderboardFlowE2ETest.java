package ru.nsu.waste.removal.ordering.service.core.facade;

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
import ru.nsu.waste.removal.ordering.service.app.view.UserLeaderboardViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.user.LeaderboardPeriod;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class UserLeaderboardFlowE2ETest {

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
    private UserFacade userFacade;

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
    void getLeaderboard_whenCurrentUserIsOutsideTop10_returnsOutsideTopRow() {
        List<Long> users = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            users.add(createSocializer("7900000000" + i, "630000"));
        }

        for (int i = 0; i < 11; i++) {
            addPointsEvent(users.get(i), 100L - i, "2026-03-19T10:00:00+00:00");
        }
        long targetUserId = users.get(11);

        UserLeaderboardViewModel leaderboard = userFacade.getLeaderboard(targetUserId, LeaderboardPeriod.WEEK);

        assertEquals(10, leaderboard.entries().size());
        assertNotNull(leaderboard.currentUserOutsideTop());
        assertEquals(targetUserId, leaderboard.currentUserOutsideTop().userId());
        assertEquals(12, leaderboard.currentUserOutsideTop().rankPosition());

        UserLeaderboardViewModel monthly = userFacade.getLeaderboard(targetUserId, LeaderboardPeriod.MONTH);
        assertEquals("MONTH", monthly.periodKey());
    }

    @Test
    void getLeaderboard_whenOpened_emitsEventAndUnlocksSocialAchievementsByRank() {
        long firstUserId = createSocializer("79000000101", "630001");
        long secondUserId = createSocializer("79000000102", "630001");
        long thirdUserId = createSocializer("79000000103", "630001");

        addPointsEvent(firstUserId, 120L, "2026-03-19T10:00:00+00:00");
        addPointsEvent(secondUserId, 100L, "2026-03-19T10:05:00+00:00");
        addPointsEvent(thirdUserId, 80L, "2026-03-19T10:10:00+00:00");

        UserLeaderboardViewModel leaderboard = userFacade.getLeaderboard(secondUserId, LeaderboardPeriod.WEEK);
        assertNull(leaderboard.currentUserOutsideTop());

        processUntilNoPendingEvents();

        assertEquals(1, countEventsByType(secondUserId, "LEADERBOARD_OPENED"));
        assertEquals(1, countUserAchievementByCode(secondUserId, "SOC_OPEN_LEADERBOARD"));
        assertEquals(1, countUserAchievementByCode(secondUserId, "SOC_TOP10_WEEK"));
        assertEquals(1, countUserAchievementByCode(secondUserId, "SOC_TOP3_WEEK"));
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

    private int countEventsByType(long userId, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_action_history where user_id = ? and event_type = ?",
                Integer.class,
                userId,
                eventType
        );
        return count == null ? 0 : count;
    }

    private int countUserAchievementByCode(long userId, String achievementCode) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from achievement_user au
                                 join achievement a on a.id = au.achievement_id
                        where au.user_id = ?
                          and a.code = ?
                        """,
                Integer.class,
                userId,
                achievementCode
        );
        return count == null ? 0 : count;
    }

    private void addPointsEvent(long userId, long points, String createdAt) {
        jdbcTemplate.update(
                """
                        insert into user_action_history(user_id, event_type, content, points_difference, created_at)
                        values (?, 'ORDER_DONE', cast('{}' as jsonb), ?, ?::timestamptz)
                        """,
                userId,
                points,
                createdAt
        );
    }

    private long createSocializer(String phone, String postalCode) {
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

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC);
        }
    }
}
