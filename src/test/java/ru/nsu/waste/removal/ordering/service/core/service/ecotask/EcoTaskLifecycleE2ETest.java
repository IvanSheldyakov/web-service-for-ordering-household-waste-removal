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
import ru.nsu.waste.removal.ordering.service.core.model.ecotask.EcoTaskAssignmentStatus;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
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
class EcoTaskLifecycleE2ETest {

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
    private EcoTaskService ecoTaskService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
                    event_processor_state,
                    sorting_regularity_window,
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
    void findAllAssignmentsByUserId_whenActiveAssignmentExpired_marksExpiredAndCreatesNewActiveOne() {
        long userId = registerAchiever("77009990001");
        long initialAssignedTaskId = findLatestEcoTaskAssignmentId(userId);

        jdbcTemplate.update(
                """
                        update user_eco_task
                        set expired_at = '2026-02-01T00:00:00+00:00'
                        where id = ?
                        """,
                initialAssignedTaskId
        );

        var assignments = ecoTaskService.findAllAssignmentsByUserId(userId);

        long activeCount = assignments.stream()
                .filter(item -> item.status() == EcoTaskAssignmentStatus.ASSIGNED)
                .count();
        long expiredCount = assignments.stream()
                .filter(item -> item.status() == EcoTaskAssignmentStatus.EXPIRED)
                .count();

        assertEquals(1L, activeCount);
        assertTrue(expiredCount >= 1L);
        assertEquals(1, countActiveAssignments(userId));
    }

    private long countActiveAssignments(long userId) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from user_eco_task
                        where user_id = ?
                          and status = 'ASSIGNED'
                        """,
                Long.class,
                userId
        );
        return count == null ? 0L : count;
    }

    private long findLatestEcoTaskAssignmentId(long userId) {
        Long id = jdbcTemplate.queryForObject(
                """
                        select id
                        from user_eco_task
                        where user_id = ?
                        order by id desc
                        limit 1
                        """,
                Long.class,
                userId
        );
        if (id == null) {
            throw new IllegalStateException("No eco task assignments found");
        }
        return id;
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
            return Clock.fixed(Instant.parse("2026-03-12T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}
