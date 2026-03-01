package ru.nsu.waste.removal.ordering.service.core.service.achievement;

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
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class AchievementEventProcessingE2ETest {

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
                    user_action_history,
                    achievement_user,
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
    void processPendingEvents_whenFirstDoneSeparateOrder_unlocksAchieverOrderAchievements() {
        long userId = registerAchiever("77006660001");
        addDoneSeparateOrders(userId, 1);
        addOrderDoneEvents(userId, 1);

        int processed = userActionEventProcessorService.processPendingEvents();
        assertEquals(1, processed);

        assertEquals(1, countUserAchievementsByCode(userId, "ACH_FIRST_ORDER"));
        assertEquals(1, countUserAchievementsByCode(userId, "ACH_FIRST_SEPARATE"));
        assertEquals(0, countUserAchievementsByCode(userId, "ACH_SEPARATE_5"));
        assertEquals(2, countEventsByType(userId, UserActionEventType.ACHIEVEMENT_UNLOCKED));

        List<String> unlockedCodes = findAchievementUnlockedCodes(userId);
        assertTrue(unlockedCodes.contains("ACH_FIRST_ORDER"));
        assertTrue(unlockedCodes.contains("ACH_FIRST_SEPARATE"));
        assertEquals(2, unlockedCodes.size());
    }

    @Test
    void processPendingEvents_whenSameAchievementConditionTriggeredAgain_doesNotCreateDuplicates() {
        long userId = registerAchiever("77006660002");
        addDoneSeparateOrders(userId, 1);
        addOrderDoneEvents(userId, 2);

        int firstProcessed = userActionEventProcessorService.processPendingEvents();
        assertEquals(2, firstProcessed);

        int secondProcessed = userActionEventProcessorService.processPendingEvents();
        assertTrue(secondProcessed >= 2);

        assertEquals(1, countUserAchievementsByCode(userId, "ACH_FIRST_ORDER"));
        assertEquals(1, countUserAchievementsByCode(userId, "ACH_FIRST_SEPARATE"));
        assertEquals(2, countEventsByType(userId, UserActionEventType.ACHIEVEMENT_UNLOCKED));
    }

    private void addDoneSeparateOrders(long userId, int count) {
        OffsetDateTime baseCreatedAt = OffsetDateTime.parse("2026-02-18T11:00:00Z");
        for (int i = 0; i < count; i++) {
            OffsetDateTime createdAt = baseCreatedAt.plusHours(i);
            OffsetDateTime pickupFrom = createdAt.plusDays(1);
            OffsetDateTime pickupTo = pickupFrom.plusHours(2);

            jdbcTemplate.update(
                    """
                            insert into order_info(
                                                   user_id,
                                                   created_at,
                                                   type,
                                                   status,
                                                   pickup_from,
                                                   pickup_to,
                                                   green_chosen,
                                                   postal_code,
                                                   cost_points
                                                   )
                            values (
                                    ?,
                                    ?,
                                    'SEPARATE',
                                    'DONE',
                                    ?,
                                    ?,
                                    false,
                                    '050000',
                                    10
                                    )
                            """,
                    userId,
                    createdAt,
                    pickupFrom,
                    pickupTo
            );
        }
    }

    private void addOrderDoneEvents(long userId, int count) {
        for (int i = 0; i < count; i++) {
            userActionHistoryRepository.addEvent(
                    userId,
                    UserActionEventType.ORDER_DONE.dbName(),
                    "{}",
                    0
            );
        }
    }

    private int countEventsByType(long userId, UserActionEventType eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from user_action_history where user_id = ? and event_type = ?",
                Integer.class,
                userId,
                eventType.dbName()
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

    private List<String> findAchievementUnlockedCodes(long userId) {
        return jdbcTemplate.query(
                """
                        select content ->> 'achievementCode' as achievement_code
                        from user_action_history
                        where user_id = ?
                          and event_type = 'ACHIEVEMENT_UNLOCKED'
                        order by id asc
                        """,
                (rs, rowNum) -> rs.getString("achievement_code"),
                userId
        );
    }

    private long registerAchiever(String phone) {
        RegistrationResultViewModel result = registrationService.register(
                validForm(phone, TZ_ALMATY),
                achieverAnswers()
        );
        return result.userId();
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
