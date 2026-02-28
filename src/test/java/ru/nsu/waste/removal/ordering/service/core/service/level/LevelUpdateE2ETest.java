package ru.nsu.waste.removal.ordering.service.core.service.level;

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
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.reward.RewardApplicationResult;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;
import ru.nsu.waste.removal.ordering.service.core.service.reward.LiederGamificationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class LevelUpdateE2ETest {

    private static final String TZ_ALMATY = "Asia/Almaty";

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
    private RegistrationService registrationService;

    @Autowired
    private LiederGamificationService liederGamificationService;

    @Autowired
    private UserActionEventProcessorService userActionEventProcessorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
                    event_processor_state,
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
    void apply_whenPointsCrossNextTarget_updatesLevelAndWritesLevelUpEvent() {
        long userId = registerAchiever("77001000001");
        jdbcTemplate.update(
                "update user_info set total_points = ?, current_points = ?, habit_strength = 0 where id = ?",
                999L,
                999L,
                userId
        );

        RewardApplicationResult rewardResult = liederGamificationService.apply(
                userId,
                UserActionEventType.SEPARATE_CHOSEN,
                true
        );
        userActionEventProcessorService.processPendingEvents();
        userActionEventProcessorService.processPendingEvents();

        assertTrue(rewardResult.appliedPointsDelta() > 0);
        assertEquals(2, getCurrentLevelId(userId));
        assertEquals(1, countLevelUpEvents(userId));
        assertEquals(1, countUserAchievementsByCode(userId, "ACH_LEVEL_UP"));
        assertEquals(1, countAchievementUnlockedEvents(userId));

        Map<String, Object> event = findLatestLevelUpEvent(userId);
        assertEquals(1L, asLong(event.get("from_level_id")));
        assertEquals(2L, asLong(event.get("to_level_id")));
        assertEquals(1000L, asLong(event.get("from_required_total_points")));
        assertEquals(2000L, asLong(event.get("to_required_total_points")));
        assertEquals(999L, asLong(event.get("old_total_points")));
        assertEquals(rewardResult.newTotalPoints(), asLong(event.get("new_total_points")));
        assertEquals(Boolean.FALSE, event.get("max_reached"));
    }

    @Test
    void apply_whenCrossesHighestThreshold_emitsLevelUpOnlyOnce() {
        long userId = registerAchiever("77001000002");

        jdbcTemplate.update("update achiever_profile set level_id = ? where user_id = ?", 5, userId);
        jdbcTemplate.update(
                "update user_info set total_points = ?, current_points = ?, habit_strength = 0 where id = ?",
                4999L,
                4999L,
                userId
        );

        RewardApplicationResult firstReward = liederGamificationService.apply(
                userId,
                UserActionEventType.SEPARATE_CHOSEN,
                true
        );
        userActionEventProcessorService.processPendingEvents();
        userActionEventProcessorService.processPendingEvents();

        assertTrue(firstReward.appliedPointsDelta() > 0);
        assertEquals(5, getCurrentLevelId(userId));
        assertEquals(1, countLevelUpEvents(userId));
        assertEquals(1, countUserAchievementsByCode(userId, "ACH_LEVEL_UP"));
        assertEquals(1, countAchievementUnlockedEvents(userId));

        Map<String, Object> firstEvent = findLatestLevelUpEvent(userId);
        assertEquals(5L, asLong(firstEvent.get("from_level_id")));
        assertEquals(5L, asLong(firstEvent.get("to_level_id")));
        assertEquals(5000L, asLong(firstEvent.get("from_required_total_points")));
        assertEquals(5000L, asLong(firstEvent.get("to_required_total_points")));
        assertEquals(4999L, asLong(firstEvent.get("old_total_points")));
        assertTrue(asLong(firstEvent.get("new_total_points")) >= 5000L);
        assertEquals(Boolean.TRUE, firstEvent.get("max_reached"));

        liederGamificationService.apply(userId, UserActionEventType.SEPARATE_CHOSEN, true);
        userActionEventProcessorService.processPendingEvents();
        userActionEventProcessorService.processPendingEvents();

        assertEquals(1, countLevelUpEvents(userId));
        assertEquals(1, countUserAchievementsByCode(userId, "ACH_LEVEL_UP"));
        assertEquals(1, countAchievementUnlockedEvents(userId));
    }

    private long registerAchiever(String phone) {
        RegistrationResultViewModel result = registrationService.register(
                validForm(phone, TZ_ALMATY),
                achieverAnswers()
        );
        return result.userId();
    }

    private int getCurrentLevelId(long userId) {
        Integer levelId = jdbcTemplate.queryForObject(
                "select level_id from achiever_profile where user_id = ?",
                Integer.class,
                userId
        );
        return levelId == null ? 0 : levelId;
    }

    private int countLevelUpEvents(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_action_history where user_id = ? and event_type = 'LEVEL_UP'",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private int countAchievementUnlockedEvents(long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_action_history where user_id = ? and event_type = 'ACHIEVEMENT_UNLOCKED'",
                Integer.class,
                userId
        );
        return count == null ? 0 : count;
    }

    private int countUserAchievementsByCode(long userId, String achievementCode) {
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

    private Map<String, Object> findLatestLevelUpEvent(long userId) {
        return jdbcTemplate.queryForMap(
                """
                        select (content ->> 'fromLevelId')::bigint as from_level_id,
                               (content ->> 'toLevelId')::bigint as to_level_id,
                               (content ->> 'fromRequiredTotalPoints')::bigint as from_required_total_points,
                               (content ->> 'toRequiredTotalPoints')::bigint as to_required_total_points,
                               (content ->> 'oldTotalPoints')::bigint as old_total_points,
                               (content ->> 'newTotalPoints')::bigint as new_total_points,
                               (content ->> 'maxReached')::boolean as max_reached
                        from user_action_history
                        where user_id = ?
                          and event_type = 'LEVEL_UP'
                        order by id desc
                        limit 1
                        """,
                userId
        );
    }

    private long asLong(Object value) {
        return ((Number) value).longValue();
    }

    private RegistrationForm validForm(String phone, String timezone) {
        RegistrationForm form = new RegistrationForm();
        form.setPhone(phone);
        form.setEmail("user@example.com");
        form.setName("Ivan");
        form.setSurname("Petrov");
        form.setPatronymic("Sergeevich");
        form.setCountryCode("KZ");
        form.setRegion("Almaty Region");
        form.setCity("Almaty");
        form.setPostalCode("050000");
        form.setDetailedAddress("Abay 10");
        form.setTimezone(timezone);
        return form;
    }

    private QuizAnswerForm achieverAnswers() {
        QuizAnswerForm form = new QuizAnswerForm();
        form.setQuizId(1L);
        form.setAnswers(Map.of(
                1L, 1L,
                2L, 4L,
                3L, 7L,
                4L, 10L,
                5L, 13L,
                6L, 16L,
                7L, 19L
        ));
        return form;
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-02-18T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}
