package ru.nsu.waste.removal.ordering.service.core.service.order;

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
import ru.nsu.waste.removal.ordering.service.core.model.order.GreenSlot;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class GreenSlotServiceE2ETest {

    private static final String TZ_ALMATY = "Asia/Almaty";
    private static final ZoneId ALMATY_ZONE = ZoneId.of(TZ_ALMATY);
    private static final String PRIMARY_POSTAL_CODE = "050000";
    private static final String SECONDARY_POSTAL_CODE = "050999";

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
    private GreenSlotService greenSlotService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
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
    void getAvailableGreenSlots_returnsOnlyFutureSlotsForTodayAndTomorrowFromSameDistrict() {
        long currentUserId = registerAchiever("77001110001", PRIMARY_POSTAL_CODE);
        long neighborUserId = registerAchiever("77001110002", PRIMARY_POSTAL_CODE);
        long anotherDistrictUserId = registerAchiever("77001110003", SECONDARY_POSTAL_CODE);

        OffsetDateTime today18From = slotStartAt(2026, 2, 18, 18);
        OffsetDateTime today18To = today18From.plusHours(2);
        addOrder(neighborUserId, PRIMARY_POSTAL_CODE, "NEW", today18From, today18To, 1);

        OffsetDateTime tomorrow10From = slotStartAt(2026, 2, 19, 10);
        OffsetDateTime tomorrow10To = tomorrow10From.plusHours(2);
        addOrder(neighborUserId, PRIMARY_POSTAL_CODE, "ASSIGNED", tomorrow10From, tomorrow10To, 2);

        OffsetDateTime pastToday14From = slotStartAt(2026, 2, 18, 14);
        addOrder(neighborUserId, PRIMARY_POSTAL_CODE, "NEW", pastToday14From, pastToday14From.plusHours(2), 3);

        OffsetDateTime cancelledTomorrow12From = slotStartAt(2026, 2, 19, 12);
        addOrder(neighborUserId, PRIMARY_POSTAL_CODE, "CANCELLED", cancelledTomorrow12From, cancelledTomorrow12From.plusHours(2), 4);

        OffsetDateTime ownTomorrow14From = slotStartAt(2026, 2, 19, 14);
        addOrder(currentUserId, PRIMARY_POSTAL_CODE, "NEW", ownTomorrow14From, ownTomorrow14From.plusHours(2), 5);

        OffsetDateTime otherDistrictTomorrow16From = slotStartAt(2026, 2, 19, 16);
        addOrder(anotherDistrictUserId, SECONDARY_POSTAL_CODE, "NEW", otherDistrictTomorrow16From, otherDistrictTomorrow16From.plusHours(2), 6);

        List<GreenSlot> slots = greenSlotService.getAvailableGreenSlots(currentUserId);

        assertEquals(2, slots.size());
        assertEquals(today18From.toInstant(), slots.get(0).pickupFrom().toInstant());
        assertEquals(today18To.toInstant(), slots.get(0).pickupTo().toInstant());
        assertEquals(tomorrow10From.toInstant(), slots.get(1).pickupFrom().toInstant());
        assertEquals(tomorrow10To.toInstant(), slots.get(1).pickupTo().toInstant());

        OffsetDateTime nowInUserZone = Instant.parse("2026-02-18T10:15:30Z").atZone(ALMATY_ZONE).toOffsetDateTime();
        assertTrue(slots.stream().allMatch(slot -> slot.pickupFrom().isAfter(nowInUserZone)));
    }

    @Test
    void getAvailableGreenSlots_whenNoPlannedOrders_returnsEmptyList() {
        long currentUserId = registerAchiever("77002220001", PRIMARY_POSTAL_CODE);
        registerAchiever("77002220002", PRIMARY_POSTAL_CODE);

        List<GreenSlot> slots = greenSlotService.getAvailableGreenSlots(currentUserId);

        assertTrue(slots.isEmpty());
    }

    private OffsetDateTime slotStartAt(int year, int month, int day, int hour) {
        return ZonedDateTime.of(year, month, day, hour, 0, 0, 0, ALMATY_ZONE).toOffsetDateTime();
    }

    private void addOrder(
            long userId,
            String postalCode,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            int createdOffsetMinutes
    ) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-02-18T10:30:00Z").plusMinutes(createdOffsetMinutes);

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
                                ?,
                                ?,
                                ?,
                                false,
                                ?,
                                10
                                )
                        """,
                userId,
                createdAt,
                status,
                pickupFrom,
                pickupTo,
                postalCode
        );
    }

    private long registerAchiever(String phone, String postalCode) {
        RegistrationResultViewModel result = registrationService.register(
                validForm(phone, TZ_ALMATY, postalCode),
                achieverAnswers()
        );
        return result.userId();
    }

    private RegistrationForm validForm(String phone, String timezone, String postalCode) {
        RegistrationForm form = new RegistrationForm();
        form.setPhone(phone);
        form.setEmail("user@example.com");
        form.setName("Ivan");
        form.setSurname("Petrov");
        form.setPatronymic("Sergeevich");
        form.setCountryCode("KZ");
        form.setRegion("Almaty Region");
        form.setCity("Almaty");
        form.setPostalCode(postalCode);
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
