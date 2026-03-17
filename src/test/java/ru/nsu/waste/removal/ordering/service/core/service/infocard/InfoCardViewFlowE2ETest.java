package ru.nsu.waste.removal.ordering.service.core.service.infocard;

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
import ru.nsu.waste.removal.ordering.service.core.model.ecoprofile.UserHistory;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.UserHistoryService;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class InfoCardViewFlowE2ETest {

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
    private InfoCardService infoCardService;

    @Autowired
    private UserActionEventProcessorService userActionEventProcessorService;

    @Autowired
    private UserHistoryService userHistoryService;

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
    void openCard_whenRepeatedViews_honorsInfoCardViewedEventAndUnlocksExplorerAchievement() {
        long userId = registerExplorer("77003330001");
        long cardId = findAnyInfoCardId();

        for (int i = 0; i < 5; i++) {
            infoCardService.openCard(userId, cardId);
        }

        processUntilNoPendingEvents();

        assertEquals(5, countEventsByType(userId, "INFO_CARD_VIEWED"));
        assertEquals(1, countUserAchievementByCode(userId, "EXP_CARDS_5"));

        UserHistory history = userHistoryService.getUserHistory(userId, 10);
        assertTrue(history.items().stream().anyMatch(item -> item.description().contains("Просмотрена карточка")));
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

    private long findAnyInfoCardId() {
        Long id = jdbcTemplate.queryForObject("select id from info_card order by id asc limit 1", Long.class);
        if (id == null) {
            throw new IllegalStateException("No info cards found");
        }
        return id;
    }

    private long registerExplorer(String phone) {
        UserRegistrationResult result = registrationService.register(
                validForm(phone, TZ_ALMATY),
                explorerAnswers()
        );
        return result.userId();
    }

    private RegistrationForm validForm(String phone, String timezone) {
        RegistrationForm form = new RegistrationForm();
        form.setPhone(phone);
        form.setEmail("user@example.com");
        form.setPassword("secret123");
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

    private QuizAnswerForm explorerAnswers() {
        QuizAnswerForm form = new QuizAnswerForm();
        form.setQuizId(1L);
        form.setAnswers(Map.of(
                1L, 3L,
                2L, 6L,
                3L, 9L,
                4L, 12L,
                5L, 15L,
                6L, 18L,
                7L, 21L
        ));
        return form;
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
