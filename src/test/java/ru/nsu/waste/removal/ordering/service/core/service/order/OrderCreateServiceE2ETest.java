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
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRegistrationResult;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.SlotOption;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventProcessorService;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class OrderCreateServiceE2ETest {

    private static final String TZ_ALMATY = "Asia/Almaty";
    private static final String PRIMARY_POSTAL_CODE = "050000";
    private static final String OTHER_POSTAL_CODE = "050111";

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
    private OrderCreateService orderCreateService;

    @Autowired
    private GreenSlotService greenSlotService;

    @Autowired
    private UserActionEventProcessorService userActionEventProcessorService;

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
                    user_eco_task,
                    achiever_profile,
                    user_info,
                    address,
                    person_info
                restart identity cascade
                """);
    }

    @Test
    void createOrder_whenMixedAndRegularSlot_createsOrderWithoutFractionsAndOnlyOrderCreatedEvent() {
        long userId = registerAchiever("77008880001", PRIMARY_POSTAL_CODE);
        registerAchiever("77008880002", OTHER_POSTAL_CODE);

        SlotOption regularSlot = findRegularSlot(userId);
        long orderId = orderCreateService.createOrder(
                userId,
                new OrderCreateCommand("MIXED", regularSlot.key(), List.of(999L))
        );

        assertOrderRow(orderId, "MIXED", "NEW", regularSlot, false, PRIMARY_POSTAL_CODE);
        assertEquals(0L, countOrderFractions(orderId));

        assertEquals(1L, countEvents(userId, UserActionEventType.ORDER_CREATED));
        assertEquals(0L, countEvents(userId, UserActionEventType.SEPARATE_CHOSEN));
        assertEquals(0L, countEvents(userId, UserActionEventType.GREEN_SLOT_CHOSEN));
    }

    @Test
    void createOrder_whenSeparateAndRegularSlot_createsFractionsAndSeparateEvent() {
        long userId = registerAchiever("77008880003", PRIMARY_POSTAL_CODE);

        SlotOption regularSlot = findRegularSlot(userId);
        List<Long> fractions = findTwoActiveFractionIds();

        long orderId = orderCreateService.createOrder(
                userId,
                new OrderCreateCommand("SEPARATE", regularSlot.key(), fractions)
        );

        assertOrderRow(orderId, "SEPARATE", "NEW", regularSlot, false, PRIMARY_POSTAL_CODE);
        assertEquals(2L, countOrderFractions(orderId));

        assertEquals(1L, countEvents(userId, UserActionEventType.ORDER_CREATED));
        assertEquals(1L, countEvents(userId, UserActionEventType.SEPARATE_CHOSEN));
        assertEquals(0L, countEvents(userId, UserActionEventType.GREEN_SLOT_CHOSEN));
    }

    @Test
    void createOrder_whenSeparateAndGreenSlot_setsGreenAndTriggersRewardableEvents() {
        long userId = registerAchiever("77008880004", PRIMARY_POSTAL_CODE);
        long neighborId = registerAchiever("77008880005", PRIMARY_POSTAL_CODE);

        SlotOption targetSlot = findRegularSlot(userId);
        addNeighborOrder(neighborId, PRIMARY_POSTAL_CODE, "NEW", targetSlot.pickupFrom(), targetSlot.pickupTo(), 1);

        SlotOption greenSlot = greenSlotService.getSlotOptions(userId).stream()
                .filter(slot -> slot.pickupFrom().toInstant().equals(targetSlot.pickupFrom().toInstant()))
                .filter(slot -> slot.pickupTo().toInstant().equals(targetSlot.pickupTo().toInstant()))
                .filter(SlotOption::green)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Green slot was expected"));

        long orderId = orderCreateService.createOrder(
                userId,
                new OrderCreateCommand("SEPARATE", greenSlot.key(), findTwoActiveFractionIds())
        );

        assertOrderRow(orderId, "SEPARATE", "NEW", greenSlot, true, PRIMARY_POSTAL_CODE);
        assertEquals(1L, countEvents(userId, UserActionEventType.ORDER_CREATED));
        assertEquals(1L, countEvents(userId, UserActionEventType.SEPARATE_CHOSEN));
        assertEquals(1L, countEvents(userId, UserActionEventType.GREEN_SLOT_CHOSEN));

        userActionEventProcessorService.processPendingEvents();
        assertTrue(findUserTotalPoints(userId) > 0L);
        assertTrue(findUserCurrentPoints(userId) > 0L);
    }

    @Test
    void createOrder_whenInvalidInput_throwsAndDoesNotCreateOrder() {
        long userId = registerAchiever("77008880006", PRIMARY_POSTAL_CODE);
        SlotOption regularSlot = findRegularSlot(userId);
        long before = countOrdersByUser(userId);

        assertThrows(
                IllegalStateException.class,
                () -> orderCreateService.createOrder(
                        userId,
                        new OrderCreateCommand("SEPARATE", regularSlot.key(), List.of())
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> orderCreateService.createOrder(
                        userId,
                        new OrderCreateCommand(
                                "MIXED",
                                "2026-03-20T10:00:00+06:00|2026-03-20T12:00:00+06:00",
                                List.of()
                        )
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> orderCreateService.createOrder(
                        userId,
                        new OrderCreateCommand("SEPARATE", regularSlot.key(), List.of(987654321L))
                )
        );

        assertEquals(before, countOrdersByUser(userId));
        assertEquals(0L, countEvents(userId, UserActionEventType.ORDER_CREATED));
    }

    @Test
    void createOrder_storesClusterKeySnapshotInOrderInfoPostalCode() {
        long userId = registerAchiever("77008880007", PRIMARY_POSTAL_CODE);

        SlotOption firstSlot = findRegularSlot(userId);
        long firstOrderId = orderCreateService.createOrder(
                userId,
                new OrderCreateCommand("MIXED", firstSlot.key(), List.of())
        );

        updateUserPostalCode(userId, OTHER_POSTAL_CODE);

        SlotOption secondSlot = findRegularSlot(userId);
        long secondOrderId = orderCreateService.createOrder(
                userId,
                new OrderCreateCommand("MIXED", secondSlot.key(), List.of())
        );

        assertEquals(PRIMARY_POSTAL_CODE, findOrderPostalCode(firstOrderId));
        assertEquals(OTHER_POSTAL_CODE, findOrderPostalCode(secondOrderId));
    }

    private SlotOption findRegularSlot(long userId) {
        return greenSlotService.getSlotOptions(userId).stream()
                .filter(slot -> !slot.green())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Regular slot was expected"));
    }

    private void assertOrderRow(
            long orderId,
            String expectedType,
            String expectedStatus,
            SlotOption expectedSlot,
            boolean expectedGreen,
            String expectedPostalCode
    ) {
        OrderRow row = jdbcTemplate.queryForObject(
                """
                        select id,
                               type,
                               status,
                               pickup_from,
                               pickup_to,
                               green_chosen,
                               postal_code,
                               cost_points
                        from order_info
                        where id = ?
                        order by created_at desc
                        limit 1
                        """,
                (rs, rowNum) -> new OrderRow(
                        rs.getLong("id"),
                        rs.getString("type"),
                        rs.getString("status"),
                        rs.getObject("pickup_from", OffsetDateTime.class),
                        rs.getObject("pickup_to", OffsetDateTime.class),
                        rs.getBoolean("green_chosen"),
                        rs.getString("postal_code"),
                        rs.getLong("cost_points")
                ),
                orderId
        );

        if (row == null) {
            throw new IllegalStateException("Order row is not found");
        }

        assertEquals(expectedType, row.type());
        assertEquals(expectedStatus, row.status());
        assertEquals(expectedSlot.pickupFrom().toInstant(), row.pickupFrom().toInstant());
        assertEquals(expectedSlot.pickupTo().toInstant(), row.pickupTo().toInstant());
        assertEquals(expectedGreen, row.greenChosen());
        assertEquals(expectedPostalCode, row.postalCode());
        assertTrue(row.costPoints() > 0L);
    }

    private List<Long> findTwoActiveFractionIds() {
        List<Long> ids = jdbcTemplate.queryForList(
                """
                        select id
                        from waste_fraction
                        where is_active = true
                        order by id
                        limit 2
                        """,
                Long.class
        );
        if (ids.size() < 2) {
            throw new IllegalStateException("Expected at least two active fractions");
        }
        return ids;
    }

    private long countOrderFractions(long orderId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from order_waste_fraction where order_id = ?",
                Long.class,
                orderId
        );
        return count == null ? 0L : count;
    }

    private long countOrdersByUser(long userId) {
        Long count = jdbcTemplate.queryForObject(
                "select count(*) from order_info where user_id = ?",
                Long.class,
                userId
        );
        return count == null ? 0L : count;
    }

    private long countEvents(long userId, UserActionEventType eventType) {
        Long count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from user_action_history
                        where user_id = ?
                          and event_type = ?
                        """,
                Long.class,
                userId,
                eventType.dbName()
        );
        return count == null ? 0L : count;
    }

    private void updateUserPostalCode(long userId, String newPostalCode) {
        jdbcTemplate.update(
                """
                        update address a
                        set postal_code = ?
                        from user_info ui
                        where ui.address_id = a.id
                          and ui.id = ?
                        """,
                newPostalCode,
                userId
        );
    }

    private String findOrderPostalCode(long orderId) {
        return jdbcTemplate.queryForObject(
                """
                        select postal_code
                        from order_info
                        where id = ?
                        order by created_at desc
                        limit 1
                        """,
                String.class,
                orderId
        );
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

    private void addNeighborOrder(
            long userId,
            String postalCode,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            int createdOffsetMinutes
    ) {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-02T10:30:00Z").plusMinutes(createdOffsetMinutes);

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
                pickupTo,
                postalCode
        );
    }

    private long registerAchiever(String phone, String postalCode) {
        UserRegistrationResult result = registrationService.register(
                validForm(phone, TZ_ALMATY, postalCode),
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

    private record OrderRow(
            long id,
            String type,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo,
            boolean greenChosen,
            String postalCode,
            long costPoints
    ) {
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-03-02T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}

