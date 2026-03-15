package ru.nsu.waste.removal.ordering.service.core.service.ecotask;

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
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.repository.history.UserActionHistoryRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.history.param.AddEventParams;
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
class EcoTaskEventProcessingE2ETest {

    private static final String TZ_ALMATY = "Asia/Almaty";
    private static final String ACHIEVER_TASK_CODE = "TASK_ACH_SEPARATE_5_WEEK";
    private static final long INITIAL_USER_POINTS = 1000L;

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
    void processPendingEvents_whenEcoTaskConditionReached_emitsCompletionEventAndAppliesPointsViaEvent() {
        long userId = registerAchiever("77008887766");
        addDoneSeparateOrders(userId, 5);
        addOrderDoneEvents(userId, 5);

        int firstProcessed = userActionEventProcessorService.processPendingEvents();

        assertEquals(5, firstProcessed);
        assertTrue(hasEcoTaskAssignmentStatus(userId, ACHIEVER_TASK_CODE, "DONE"));
        assertEquals(1, countEventsByType(userId, UserActionEventType.ECO_TASK_COMPLETED));
        assertEquals(100L, findLatestEcoTaskCompletedPointsDifference(userId));
        assertEquals(INITIAL_USER_POINTS, findUserTotalPoints(userId));
        assertEquals(INITIAL_USER_POINTS, findUserCurrentPoints(userId));

        int secondProcessed = processUntilNoPendingEvents();
        assertTrue(secondProcessed >= 1);

        assertEquals(1, countEventsByType(userId, UserActionEventType.ECO_TASK_COMPLETED));
        assertEquals(1, countEventsByType(userId, UserActionEventType.ECO_TASK_REWARD_REQUEST));

        long ecoTaskBaseDelta = findLatestPointsDifferenceByType(userId, UserActionEventType.ECO_TASK_COMPLETED);
        long ecoTaskAdaptiveDelta = findLatestPointsDifferenceByType(userId, UserActionEventType.ECO_TASK_REWARD_REQUEST);
        long regularityAdaptiveDelta = findLatestPointsDifferenceByType(
                userId,
                UserActionEventType.SORTING_REGULARITY_CONFIRMED
        );

        long expectedTotal = INITIAL_USER_POINTS + ecoTaskBaseDelta + ecoTaskAdaptiveDelta + regularityAdaptiveDelta;
        assertEquals(expectedTotal, findUserTotalPoints(userId));
        assertEquals(expectedTotal, findUserCurrentPoints(userId));
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
                                                   completed_at,
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
                    createdAt.plusMinutes(30),
                    pickupFrom,
                    pickupTo
            );
        }
    }

    private void addOrderDoneEvents(long userId, int count) {
        for (int i = 0; i < count; i++) {
            userActionHistoryRepository.addEvent(new AddEventParams(
                    userId,
                    UserActionEventType.ORDER_DONE.dbName(),
                    "{}",
                    0
            ));
        }
    }

    private boolean hasEcoTaskAssignmentStatus(long userId, String ecoTaskCode, String status) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from user_eco_task uet
                                 join eco_task et on et.id = uet.eco_task_id
                        where uet.user_id = ?
                          and et.code = ?
                          and uet.status = ?
                        """,
                Integer.class,
                userId,
                ecoTaskCode,
                status
        );
        return count != null && count > 0;
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

    private long findLatestEcoTaskCompletedPointsDifference(long userId) {
        return findLatestPointsDifferenceByType(userId, UserActionEventType.ECO_TASK_COMPLETED);
    }

    private long findLatestPointsDifferenceByType(long userId, UserActionEventType eventType) {
        List<Long> pointsDifferences = jdbcTemplate.query(
                """
                        select points_difference
                        from user_action_history
                        where user_id = ?
                          and event_type = ?
                        order by id desc
                        limit 1
                        """,
                (rs, rowNum) -> rs.getLong("points_difference"),
                userId,
                eventType.dbName()
        );
        if (pointsDifferences.isEmpty()) {
            return 0L;
        }
        return pointsDifferences.getFirst();
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

    private long findUserTotalPoints(long userId) {
        Long totalPoints = jdbcTemplate.queryForObject(
                "select total_points from user_info where id = ?",
                Long.class,
                userId
        );
        return totalPoints == null ? 0L : totalPoints;
    }

    private long findUserCurrentPoints(long userId) {
        Long currentPoints = jdbcTemplate.queryForObject(
                "select current_points from user_info where id = ?",
                Long.class,
                userId
        );
        return currentPoints == null ? 0L : currentPoints;
    }

    private long registerAchiever(String phone) {
        UserRegistrationResult result = registrationService.register(
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

