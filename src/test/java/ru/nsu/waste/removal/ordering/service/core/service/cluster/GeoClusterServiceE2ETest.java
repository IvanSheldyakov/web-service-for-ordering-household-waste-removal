package ru.nsu.waste.removal.ordering.service.core.service.cluster;

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
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterContext;
import ru.nsu.waste.removal.ordering.service.core.model.cluster.GeoClusterKey;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class GeoClusterServiceE2ETest {

    private static final String TZ_ALMATY = "Asia/Almaty";
    private static final String TZ_OMSK = "Asia/Omsk";
    private static final String PRIMARY_POSTAL_CODE = "050000";
    private static final String SECONDARY_POSTAL_CODE = "050777";

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
    private GeoClusterService geoClusterService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetRuntimeData() {
        jdbcTemplate.execute("""
                truncate table
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
    void getUserClusterContext_returnsPostalCodeAsClusterKeyAndTimezone() {
        long userId = registerAchiever("77005550001", TZ_ALMATY, PRIMARY_POSTAL_CODE);

        GeoClusterContext clusterContext = geoClusterService.getUserClusterContext(userId);

        assertEquals(PRIMARY_POSTAL_CODE, clusterContext.clusterKey().value());
        assertEquals(TZ_ALMATY, clusterContext.timezone());
        assertEquals(PRIMARY_POSTAL_CODE, geoClusterService.getClusterKeyByUserId(userId).value());
    }

    @Test
    void belongsToSameCluster_usesExactPostalCodeEqualityInMvp() {
        long userInPrimary1 = registerAchiever("77005550002", TZ_ALMATY, PRIMARY_POSTAL_CODE);
        long userInPrimary2 = registerAchiever("77005550003", TZ_OMSK, PRIMARY_POSTAL_CODE);
        long userInSecondary = registerAchiever("77005550004", TZ_ALMATY, SECONDARY_POSTAL_CODE);

        GeoClusterKey primaryFirst = geoClusterService.getClusterKeyByUserId(userInPrimary1);
        GeoClusterKey primarySecond = geoClusterService.getClusterKeyByUserId(userInPrimary2);
        GeoClusterKey secondary = geoClusterService.getClusterKeyByUserId(userInSecondary);

        assertTrue(geoClusterService.belongsToSameCluster(primaryFirst, primarySecond));
        assertFalse(geoClusterService.belongsToSameCluster(primaryFirst, secondary));
    }

    @Test
    void countActiveOrdersInCluster_countsOnlyNewAndAssignedFromSameCluster() {
        long firstUser = registerAchiever("77005550005", TZ_ALMATY, PRIMARY_POSTAL_CODE);
        long secondUser = registerAchiever("77005550006", TZ_ALMATY, PRIMARY_POSTAL_CODE);

        OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00+06:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-05-03T00:00:00+06:00");

        addOrder(firstUser, PRIMARY_POSTAL_CODE, "NEW", OffsetDateTime.parse("2026-05-01T10:00:00+06:00"), 1);
        addOrder(secondUser, PRIMARY_POSTAL_CODE, "ASSIGNED", OffsetDateTime.parse("2026-05-01T12:00:00+06:00"), 2);
        addOrder(secondUser, PRIMARY_POSTAL_CODE, "DONE", OffsetDateTime.parse("2026-05-01T14:00:00+06:00"), 3);
        addOrder(secondUser, PRIMARY_POSTAL_CODE, "CANCELLED", OffsetDateTime.parse("2026-05-01T16:00:00+06:00"), 4);
        addOrder(secondUser, SECONDARY_POSTAL_CODE, "NEW", OffsetDateTime.parse("2026-05-01T18:00:00+06:00"), 5);

        long count = geoClusterService.countActiveOrdersInCluster(
                new GeoClusterKey(PRIMARY_POSTAL_CODE),
                from,
                to
        );

        assertEquals(2L, count);
    }

    private void addOrder(
            long userId,
            String postalCode,
            String status,
            OffsetDateTime pickupFrom,
            int createdOffsetMinutes
    ) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-01T06:00:00Z").plusMinutes(createdOffsetMinutes);

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
                        values (?, ?, 'MIXED', ?, ?, ?, false, ?, 100)
                        """,
                userId,
                createdAt,
                status,
                pickupFrom,
                pickupFrom.plusHours(2),
                postalCode
        );
    }

    private long registerAchiever(String phone, String timezone, String postalCode) {
        UserRegistrationResult result = registrationService.register(
                validForm(phone, timezone, postalCode),
                achieverAnswers()
        );
        return result.userId();
    }

    private RegistrationForm validForm(String phone, String timezone, String postalCode) {
        RegistrationForm form = new RegistrationForm();
        form.setPhone(phone);
        form.setEmail(phone + "@example.com");
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
}
