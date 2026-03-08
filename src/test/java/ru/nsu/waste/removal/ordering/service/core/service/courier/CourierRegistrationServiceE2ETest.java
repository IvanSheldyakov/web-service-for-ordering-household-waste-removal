package ru.nsu.waste.removal.ordering.service.core.service.courier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.nsu.waste.removal.ordering.service.app.constant.AttributeNames;
import ru.nsu.waste.removal.ordering.service.app.form.CourierLoginForm;
import ru.nsu.waste.removal.ordering.service.app.form.CourierRegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.facade.CourierRegistrationFacade;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierRegistrationResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class CourierRegistrationServiceE2ETest {

    private static final String COURIER_TIMEZONE = "Asia/Omsk";

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
    private CourierRegistrationService courierRegistrationService;

    @Autowired
    private CourierRegistrationFacade courierRegistrationFacade;

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
                    courier,
                    user_eco_task,
                    achiever_profile,
                    user_info,
                    address,
                    person_info
                restart identity cascade
                """);
    }

    @Test
    void register_createsPersonAndCourierWithPostalCodeAndTimezone() {
        CourierRegistrationForm form = validCourierForm("79001112233", "630000");

        CourierRegistrationResult result = courierRegistrationService.register(form);

        assertNotNull(result);
        assertTrue(result.courierId() > 0L);
        assertEquals("630000", result.postalCode());
        assertEquals(0L, result.totalPoints());

        Long personCount = jdbcTemplate.queryForObject(
                "select count(*) from person_info where phone = ?",
                Long.class,
                79001112233L
        );
        assertEquals(1L, personCount == null ? 0L : personCount);

        CourierRow row = jdbcTemplate.queryForObject(
                """
                        select c.id, c.postal_code, c.timezone, c.total_points
                        from courier c
                                 join person_info p on p.id = c.person_id
                        where p.phone = ?
                        """,
                (rs, rowNum) -> new CourierRow(
                        rs.getLong("id"),
                        rs.getString("postal_code"),
                        rs.getString("timezone"),
                        rs.getLong("total_points")
                ),
                79001112233L
        );

        if (row == null) {
            throw new IllegalStateException("Courier row was not created");
        }

        assertEquals(result.courierId(), row.id());
        assertEquals("630000", row.postalCode());
        assertEquals(COURIER_TIMEZONE, row.timezone());
        assertEquals(0L, row.totalPoints());
    }

    @Test
    void login_findsCourierByPhone_andReturnsValidationErrorForUnknownPhone() {
        CourierRegistrationResult createdCourier = courierRegistrationService.register(
                validCourierForm("79002223344", "630001")
        );

        CourierLoginForm successForm = new CourierLoginForm();
        successForm.setPhone("79002223344");
        BindingResult successBinding = new BeanPropertyBindingResult(
                successForm,
                AttributeNames.COURIER_LOGIN_FORM
        );

        Long foundCourierId = courierRegistrationFacade.login(successForm, successBinding);

        assertEquals(createdCourier.courierId(), foundCourierId);
        assertFalse(successBinding.hasErrors());

        CourierLoginForm errorForm = new CourierLoginForm();
        errorForm.setPhone("79009998877");
        BindingResult errorBinding = new BeanPropertyBindingResult(
                errorForm,
                AttributeNames.COURIER_LOGIN_FORM
        );

        Long notFoundCourierId = courierRegistrationFacade.login(errorForm, errorBinding);

        assertNull(notFoundCourierId);
        assertTrue(errorBinding.hasFieldErrors("phone"));
    }

    private CourierRegistrationForm validCourierForm(String phone, String postalCode) {
        CourierRegistrationForm form = new CourierRegistrationForm();
        form.setPhone(phone);
        form.setEmail(phone + "@courier.test");
        form.setName("Иван");
        form.setSurname("Петров");
        form.setPatronymic("Игоревич");
        form.setPostalCode(postalCode);
        form.setTimezone(COURIER_TIMEZONE);
        return form;
    }

    private record CourierRow(
            long id,
            String postalCode,
            String timezone,
            long totalPoints
    ) {
    }
}
