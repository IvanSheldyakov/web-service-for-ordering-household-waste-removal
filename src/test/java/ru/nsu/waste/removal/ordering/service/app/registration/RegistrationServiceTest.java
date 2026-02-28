package ru.nsu.waste.removal.ordering.service.app.registration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
import ru.nsu.waste.removal.ordering.service.core.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class RegistrationServiceTest {

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
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
                    user_eco_task,
                    achiever_profile,
                    user_info,
                    address,
                    person_info
                restart identity cascade
                """);
    }

    @Test
    void happyPath_createsExpectedRecords_andAchieverProfileOnlyForAchiever() {
        int personBefore = count("person_info");
        int addressBefore = count("address");
        int userBefore = count("user_info");
        int achieverBefore = count("achiever_profile");
        int userEcoTaskBefore = count("user_eco_task");

        RegistrationForm form = validForm("77001234567", TZ_ALMATY);
        QuizAnswerForm answers = achieverAnswers();

        RegistrationResultViewModel result = registrationService.register(form, answers);

        assertNotNull(result);
        assertEquals("Достигатель", result.userTypeName());
        assertEquals(0, result.balances().totalPoints());
        assertEquals(0, result.balances().currentPoints());

        assertEquals(personBefore + 1, count("person_info"));
        assertEquals(addressBefore + 1, count("address"));
        assertEquals(userBefore + 1, count("user_info"));
        assertEquals(achieverBefore + 1, count("achiever_profile"));
        assertEquals(userEcoTaskBefore + 1, count("user_eco_task"));

        int assignedTasks = jdbcTemplate.queryForObject(
                "select count(*) from user_eco_task where user_id = ? and status = 'ASSIGNED'",
                Integer.class,
                result.userId()
        );
        assertEquals(1, assignedTasks);
    }

    @Test
    void phoneDuplicate_registrationFails_andNoPartialRowsInserted() {
        RegistrationForm firstForm = validForm("77005554433", TZ_ALMATY);
        registrationService.register(firstForm, achieverAnswers());

        int personBefore = count("person_info");
        int addressBefore = count("address");
        int userBefore = count("user_info");
        int ecoTaskBefore = count("user_eco_task");

        RegistrationForm secondForm = validForm("77005554433", TZ_ALMATY);
        secondForm.setEmail("another@example.com");

        assertThrows(
                DuplicatePhoneException.class,
                () -> registrationService.register(secondForm, explorerAnswers())
        );

        assertEquals(personBefore, count("person_info"));
        assertEquals(addressBefore, count("address"));
        assertEquals(userBefore, count("user_info"));
        assertEquals(ecoTaskBefore, count("user_eco_task"));
    }

    @Test
    void tie_resolvedByTieBreakQuestion() {
        int achieverBefore = count("achiever_profile");

        RegistrationForm form = validForm("77001112233", TZ_ALMATY);
        QuizAnswerForm tieAnswers = tieWithExplorerTieBreakAnswers();

        RegistrationResultViewModel result = registrationService.register(form, tieAnswers);

        assertEquals("Исследователь", result.userTypeName());
        assertEquals(achieverBefore, count("achiever_profile"));
    }

    @Test
    void expiredAt_weeklyAndMonthly_areCalculatedInUserTimezone() {
        RegistrationForm form = validForm("77009998877", TZ_ALMATY);
        RegistrationResultViewModel result = registrationService.register(form, explorerAnswers());
        long userId = result.userId();

        OffsetDateTime weeklyExpired = jdbcTemplate.queryForObject(
                """
                        select uet.expired_at from user_eco_task uet
                        join eco_task et on et.id = uet.eco_task_id
                        where uet.user_id = ? and et.code = 'TASK_EXP_OPEN_PROFILE_WEEK'
                        """,
                OffsetDateTime.class,
                userId
        );
        OffsetDateTime monthlyExpired = jdbcTemplate.queryForObject(
                """
                        select uet.expired_at from user_eco_task uet
                        join eco_task et on et.id = uet.eco_task_id
                        where uet.user_id = ? and et.code = 'TASK_EXP_FRACTIONS_3_MONTH'
                        """,
                OffsetDateTime.class,
                userId
        );

        ZoneId almaty = ZoneId.of(TZ_ALMATY);
        OffsetDateTime expectedWeekly = ZonedDateTime.of(
                2026, 2, 22, 23, 59, 59, 0, almaty
        ).toOffsetDateTime();
        OffsetDateTime expectedMonthly = ZonedDateTime.of(
                2026, 2, 28, 23, 59, 59, 0, almaty
        ).toOffsetDateTime();

        assertNotNull(weeklyExpired);
        assertNotNull(monthlyExpired);
        assertEquals(expectedWeekly.toInstant(), weeklyExpired.toInstant());
        assertEquals(expectedMonthly.toInstant(), monthlyExpired.toInstant());
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
        return answers(Map.of(
                1L, 1L,
                2L, 4L,
                3L, 7L,
                4L, 10L,
                5L, 13L,
                6L, 16L,
                7L, 19L
        ));
    }

    private QuizAnswerForm explorerAnswers() {
        return answers(Map.of(
                1L, 3L,
                2L, 6L,
                3L, 9L,
                4L, 12L,
                5L, 15L,
                6L, 18L,
                7L, 21L
        ));
    }

    private QuizAnswerForm tieWithExplorerTieBreakAnswers() {
        return answers(Map.of(
                1L, 1L,
                2L, 4L,
                3L, 7L,
                4L, 12L,
                5L, 15L,
                6L, 17L,
                7L, 21L
        ));
    }

    private QuizAnswerForm answers(Map<Long, Long> answersByQuestion) {
        QuizAnswerForm form = new QuizAnswerForm();
        form.setQuizId(1L);
        form.setAnswers(answersByQuestion);
        return form;
    }

    private int count(String table) {
        Integer value = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        return value == null ? 0 : value;
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
