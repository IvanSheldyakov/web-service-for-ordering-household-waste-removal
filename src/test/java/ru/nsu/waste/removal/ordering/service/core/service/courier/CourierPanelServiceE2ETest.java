package ru.nsu.waste.removal.ordering.service.core.service.courier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.nsu.waste.removal.ordering.service.app.form.CourierRegistrationForm;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class CourierPanelServiceE2ETest {

    private static final String TZ_OMSK = "Asia/Omsk";
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.parse("2026-03-20T10:15:30Z");

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
    private CourierPanelService courierPanelService;

    @Autowired
    private OrderInfoService orderInfoService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

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
    void getPanel_returnsOnlyOrdersRelevantForCourier() {
        long courierId = registerCourier("79030000001", "630000");
        long anotherCourierId = registerCourier("79030000002", "630001");

        long userInCourierArea = createUser("79100000001", "630000", "Новосибирск", "Ленина, 1");
        long userOtherArea = createUser("79100000002", "630001", "Томск", "Мира, 2");

        OrderKey availableOrder = addOrder(
                userInCourierArea,
                "MIXED",
                "NEW",
                "630000",
                null,
                OffsetDateTime.parse("2026-03-10T08:00:00Z")
        );
        addOrder(
                userOtherArea,
                "MIXED",
                "NEW",
                "630001",
                null,
                OffsetDateTime.parse("2026-03-10T09:00:00Z")
        );
        OrderKey assignedToCourier = addOrder(
                userInCourierArea,
                "SEPARATE",
                "ASSIGNED",
                "630000",
                courierId,
                OffsetDateTime.parse("2026-03-10T10:00:00Z")
        );
        addOrderFraction(assignedToCourier, "PAPER");
        addOrder(
                userInCourierArea,
                "MIXED",
                "ASSIGNED",
                "630000",
                anotherCourierId,
                OffsetDateTime.parse("2026-03-10T11:00:00Z")
        );

        CourierPanel panel = courierPanelService.getPanel(courierId);

        assertEquals(courierId, panel.courierId());
        assertEquals(1, panel.availableOrders().size());
        assertEquals(availableOrder.id(), panel.availableOrders().getFirst().orderId());
        assertEquals(1, panel.assignedOrders().size());
        assertEquals(assignedToCourier.id(), panel.assignedOrders().getFirst().orderId());
        assertEquals("Новосибирск", panel.assignedOrders().getFirst().city());
        assertFalse(panel.assignedOrders().getFirst().fractions().isEmpty());
    }

    @Test
    void takeOrder_isAtomicAndProtectsFromParallelCapture() {
        long firstCourierId = registerCourier("79030000011", "630000");
        long secondCourierId = registerCourier("79030000012", "630000");
        long userId = createUser("79100000011", "630000", "Новосибирск", "Красный проспект, 5");

        OrderKey targetOrder = addOrder(
                userId,
                "MIXED",
                "NEW",
                "630000",
                null,
                OffsetDateTime.parse("2026-03-11T08:00:00Z")
        );

        courierPanelService.takeOrder(firstCourierId, targetOrder);

        OrderState afterFirstTake = findOrderState(targetOrder);
        assertEquals("ASSIGNED", afterFirstTake.status());
        assertEquals(firstCourierId, afterFirstTake.courierId());
        assertEquals(FIXED_NOW.toInstant(), afterFirstTake.assignedAt().toInstant());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.takeOrder(secondCourierId, targetOrder)
        );
        assertEquals("Заказ уже взят другим курьером или недоступен", exception.getMessage());
    }

    @Test
    void completeOrder_marksDone_addsCourierPoints_andCreatesOrderDoneEvent() throws Exception {
        long courierId = registerCourier("79030000021", "630000");
        long userId = createUser("79100000021", "630000", "Новосибирск", "Серебренниковская, 10");

        OrderKey assignedOrder = addOrder(
                userId,
                "SEPARATE",
                "ASSIGNED",
                "630000",
                courierId,
                OffsetDateTime.parse("2026-03-12T08:00:00Z")
        );
        addOrderFraction(assignedOrder, "PAPER");
        addOrderFraction(assignedOrder, "PLASTIC");

        long doneOrdersBefore = orderInfoService.countDoneOrders(userId);
        long pointsBefore = findCourierTotalPoints(courierId);

        courierPanelService.completeOrder(courierId, assignedOrder);

        OrderState doneState = findOrderState(assignedOrder);
        assertEquals("DONE", doneState.status());
        assertEquals(courierId, doneState.courierId());
        assertNotNull(doneState.completedAt());
        assertEquals(FIXED_NOW.toInstant(), doneState.completedAt().toInstant());

        assertEquals(pointsBefore + 20L, findCourierTotalPoints(courierId));
        assertEquals(doneOrdersBefore + 1L, orderInfoService.countDoneOrders(userId));

        EventRow eventRow = jdbcTemplate.queryForObject(
                """
                        select points_difference,
                               content::text as content_json
                        from user_action_history
                        where user_id = ?
                          and event_type = ?
                        order by id desc
                        limit 1
                        """,
                (rs, rowNum) -> new EventRow(
                        rs.getLong("points_difference"),
                        rs.getString("content_json")
                ),
                userId,
                UserActionEventType.ORDER_DONE.dbName()
        );

        if (eventRow == null) {
            throw new IllegalStateException("ORDER_DONE event is missing");
        }

        assertEquals(0L, eventRow.pointsDifference());
        JsonNode content = objectMapper.readTree(eventRow.contentJson());
        assertEquals(assignedOrder.id(), content.path("orderId").asLong());
        assertEquals(courierId, content.path("courierId").asLong());
        assertEquals("DONE", content.path("status").asText());
        assertEquals(2, content.path("fractionIds").size());
    }

    @Test
    void negativeScenarios_throwExpectedErrors() {
        long firstCourierId = registerCourier("79030000031", "630000");
        long secondCourierId = registerCourier("79030000032", "630000");
        long userId = createUser("79100000031", "630000", "Новосибирск", "Гоголя, 15");

        OrderKey assignedToFirstCourier = addOrder(
                userId,
                "MIXED",
                "ASSIGNED",
                "630000",
                firstCourierId,
                OffsetDateTime.parse("2026-03-13T08:00:00Z")
        );
        OrderKey newOrder = addOrder(
                userId,
                "MIXED",
                "NEW",
                "630000",
                null,
                OffsetDateTime.parse("2026-03-13T09:00:00Z")
        );
        OrderKey doneOrder = addOrder(
                userId,
                "MIXED",
                "DONE",
                "630000",
                firstCourierId,
                OffsetDateTime.parse("2026-03-13T10:00:00Z")
        );

        IllegalStateException notOwnCompletion = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.completeOrder(secondCourierId, assignedToFirstCourier)
        );
        assertEquals("Заказ не найден или не назначен этому курьеру", notOwnCompletion.getMessage());

        IllegalStateException completeNewOrder = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.completeOrder(firstCourierId, newOrder)
        );
        assertEquals("Заказ не найден или не назначен этому курьеру", completeNewOrder.getMessage());

        IllegalStateException takeDoneOrder = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.takeOrder(firstCourierId, doneOrder)
        );
        assertEquals("Заказ уже взят другим курьером или недоступен", takeDoneOrder.getMessage());
    }

    private long registerCourier(String phone, String postalCode) {
        CourierRegistrationForm form = new CourierRegistrationForm();
        form.setPhone(phone);
        form.setEmail(phone + "@courier.test");
        form.setName("Иван");
        form.setSurname("Курьеров");
        form.setPatronymic("Андреевич");
        form.setPostalCode(postalCode);
        form.setTimezone(TZ_OMSK);
        return courierRegistrationService.register(form).courierId();
    }

    private long createUser(String phone, String postalCode, String city, String detailedAddress) {
        Long personId = jdbcTemplate.queryForObject(
                """
                        insert into person_info(phone, email, name, surname, patronymic)
                        values (?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                Long.parseLong(phone),
                phone + "@user.test",
                "Пользователь",
                "Тестовый",
                null
        );

        Long addressId = jdbcTemplate.queryForObject(
                """
                        insert into address(country_code, postal_code, city, region, detailed_address, timezone)
                        values (?, ?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                "RU",
                postalCode,
                city,
                "Новосибирская область",
                detailedAddress,
                "UTC"
        );

        Long userId = jdbcTemplate.queryForObject(
                """
                        insert into user_info(type_id, address_id, person_id)
                        values ((select id from user_type where name = 'ACHIEVER'), ?, ?)
                        returning id
                        """,
                Long.class,
                addressId,
                personId
        );

        if (personId == null || addressId == null || userId == null) {
            throw new IllegalStateException("Failed to prepare test user");
        }

        return userId;
    }

    private OrderKey addOrder(
            long userId,
            String type,
            String status,
            String postalCode,
            Long courierId,
            OffsetDateTime createdAt
    ) {
        OffsetDateTime assignedAt = ("ASSIGNED".equals(status) || "DONE".equals(status))
                ? createdAt.plusMinutes(15)
                : null;
        OffsetDateTime completedAt = "DONE".equals(status)
                ? createdAt.plusHours(1)
                : null;
        OffsetDateTime pickupFrom = createdAt.plusDays(1);
        OffsetDateTime pickupTo = pickupFrom.plusHours(2);

        OrderKey orderKey = jdbcTemplate.queryForObject(
                """
                        insert into order_info(
                                               user_id,
                                               courier_id,
                                               created_at,
                                               completed_at,
                                               assigned_at,
                                               type,
                                               status,
                                               pickup_from,
                                               pickup_to,
                                               green_chosen,
                                               postal_code,
                                               cost_points
                                               )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id, created_at
                        """,
                (rs, rowNum) -> new OrderKey(
                        rs.getLong("id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                userId,
                courierId,
                createdAt,
                completedAt,
                assignedAt,
                type,
                status,
                pickupFrom,
                pickupTo,
                true,
                postalCode,
                100L
        );

        if (orderKey == null) {
            throw new IllegalStateException("Failed to create test order");
        }

        return orderKey;
    }

    private void addOrderFraction(OrderKey orderKey, String fractionType) {
        Long fractionId = jdbcTemplate.queryForObject(
                "select id from waste_fraction where type = ?",
                Long.class,
                fractionType
        );
        if (fractionId == null) {
            throw new IllegalStateException("Fraction is not found");
        }

        jdbcTemplate.update(
                """
                        insert into order_waste_fraction(order_id, order_created_at, fraction_id)
                        values (?, ?, ?)
                        """,
                orderKey.id(),
                orderKey.createdAt(),
                fractionId
        );
    }

    private OrderState findOrderState(OrderKey orderKey) {
        return jdbcTemplate.queryForObject(
                """
                        select status, courier_id, assigned_at, completed_at
                        from order_info
                        where id = ?
                          and created_at = ?
                        """,
                (rs, rowNum) -> new OrderState(
                        rs.getString("status"),
                        rs.getLong("courier_id"),
                        rs.getObject("assigned_at", OffsetDateTime.class),
                        rs.getObject("completed_at", OffsetDateTime.class)
                ),
                orderKey.id(),
                orderKey.createdAt()
        );
    }

    private long findCourierTotalPoints(long courierId) {
        Long points = jdbcTemplate.queryForObject(
                "select total_points from courier where id = ?",
                Long.class,
                courierId
        );
        return points == null ? 0L : points;
    }

    private record OrderState(
            String status,
            long courierId,
            OffsetDateTime assignedAt,
            OffsetDateTime completedAt
    ) {
    }

    private record EventRow(
            long pointsDifference,
            String contentJson
    ) {
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-03-20T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}
