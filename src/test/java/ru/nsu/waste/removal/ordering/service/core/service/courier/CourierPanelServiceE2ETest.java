package ru.nsu.waste.removal.ordering.service.core.service.courier;

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
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroupKey;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderKey;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class CourierPanelServiceE2ETest {

    private static final String TZ_OMSK = "Asia/Omsk";
    private static final OffsetDateTime FIXED_NOW = OffsetDateTime.parse("2026-03-20T10:15:30Z");
    private static final String PRIMARY_CLUSTER = "630000";
    private static final String SECONDARY_CLUSTER = "630001";

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
    void getPanel_groupsOrdersByClusterAndSlot() {
        long courierId = registerCourier("79030000101", PRIMARY_CLUSTER);

        long userA1 = createUser("79100000101", PRIMARY_CLUSTER, "Новосибирск", "Ленина, 1");
        long userA2 = createUser("79100000102", PRIMARY_CLUSTER, "Новосибирск", "Крылова, 2");
        long userA3 = createUser("79100000103", PRIMARY_CLUSTER, "Новосибирск", "Гоголя, 3");
        long userB = createUser("79100000104", SECONDARY_CLUSTER, "Томск", "Мира, 4");

        OffsetDateTime slot1From = OffsetDateTime.parse("2026-03-21T10:00:00Z");
        OffsetDateTime slot1To = slot1From.plusHours(2);
        OffsetDateTime slot2From = OffsetDateTime.parse("2026-03-21T14:00:00Z");
        OffsetDateTime slot2To = slot2From.plusHours(2);

        OrderKey firstInGroup = addOrder(
                userA1,
                "SEPARATE",
                "NEW",
                PRIMARY_CLUSTER,
                null,
                OffsetDateTime.parse("2026-03-20T08:00:00Z"),
                slot1From,
                slot1To
        );
        OrderKey secondInGroup = addOrder(
                userA2,
                "MIXED",
                "NEW",
                PRIMARY_CLUSTER,
                null,
                OffsetDateTime.parse("2026-03-20T09:00:00Z"),
                slot1From,
                slot1To
        );
        addOrder(
                userA3,
                "MIXED",
                "NEW",
                PRIMARY_CLUSTER,
                null,
                OffsetDateTime.parse("2026-03-20T09:30:00Z"),
                slot2From,
                slot2To
        );
        addOrder(
                userB,
                "MIXED",
                "NEW",
                SECONDARY_CLUSTER,
                null,
                OffsetDateTime.parse("2026-03-20T10:00:00Z"),
                slot1From,
                slot1To
        );

        CourierPanel panel = courierPanelService.getPanel(courierId);

        assertEquals(2, panel.availableOrderGroups().size());
        assertEquals(0, panel.assignedOrderGroups().size());

        var firstGroup = panel.availableOrderGroups().get(0);
        assertEquals(PRIMARY_CLUSTER, firstGroup.clusterKey());
        assertEquals(slot1From.toInstant(), firstGroup.pickupFrom().toInstant());
        assertEquals(slot1To.toInstant(), firstGroup.pickupTo().toInstant());
        assertEquals(2, firstGroup.ordersCount());
        assertEquals(1, firstGroup.separateOrdersCount());
        assertEquals(1, firstGroup.mixedOrdersCount());
        assertEquals(firstInGroup.id(), firstGroup.orders().get(0).orderId());
        assertEquals(secondInGroup.id(), firstGroup.orders().get(1).orderId());

        var secondGroup = panel.availableOrderGroups().get(1);
        assertEquals(1, secondGroup.ordersCount());
        assertEquals(slot2From.toInstant(), secondGroup.pickupFrom().toInstant());
    }

    @Test
    void takeOrderGroup_assignsWholeGroupAtomically() {
        long courierId = registerCourier("79030000111", PRIMARY_CLUSTER);

        long user1 = createUser("79100000111", PRIMARY_CLUSTER, "Новосибирск", "Ленина, 10");
        long user2 = createUser("79100000112", PRIMARY_CLUSTER, "Новосибирск", "Ленина, 11");
        long user3 = createUser("79100000113", PRIMARY_CLUSTER, "Новосибирск", "Ленина, 12");

        OffsetDateTime slotFrom = OffsetDateTime.parse("2026-03-21T12:00:00Z");
        OffsetDateTime slotTo = slotFrom.plusHours(2);

        OrderKey order1 = addOrder(user1, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:00:00Z"), slotFrom, slotTo);
        OrderKey order2 = addOrder(user2, "SEPARATE", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:05:00Z"), slotFrom, slotTo);
        OrderKey order3 = addOrder(user3, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:10:00Z"), slotFrom, slotTo);

        courierPanelService.takeOrderGroup(
                courierId,
                new CourierOrderGroupKey(PRIMARY_CLUSTER, slotFrom, slotTo),
                3
        );

        assertAssignedToCourier(order1, courierId);
        assertAssignedToCourier(order2, courierId);
        assertAssignedToCourier(order3, courierId);

        CourierPanel panel = courierPanelService.getPanel(courierId);
        assertEquals(0, panel.availableOrderGroups().size());
        assertEquals(1, panel.assignedOrderGroups().size());
        assertEquals(3, panel.assignedOrderGroups().getFirst().ordersCount());
    }

    @Test
    void takeOrderGroup_rollsBackIfCompositionChanged() {
        long firstCourierId = registerCourier("79030000121", PRIMARY_CLUSTER);
        long secondCourierId = registerCourier("79030000122", PRIMARY_CLUSTER);

        long user1 = createUser("79100000121", PRIMARY_CLUSTER, "Новосибирск", "Каменская, 10");
        long user2 = createUser("79100000122", PRIMARY_CLUSTER, "Новосибирск", "Каменская, 11");
        long user3 = createUser("79100000123", PRIMARY_CLUSTER, "Новосибирск", "Каменская, 12");

        OffsetDateTime slotFrom = OffsetDateTime.parse("2026-03-21T16:00:00Z");
        OffsetDateTime slotTo = slotFrom.plusHours(2);

        OrderKey firstOrder = addOrder(user1, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:00:00Z"), slotFrom, slotTo);
        OrderKey secondOrder = addOrder(user2, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:05:00Z"), slotFrom, slotTo);
        OrderKey thirdOrder = addOrder(user3, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T08:10:00Z"), slotFrom, slotTo);

        courierPanelService.takeOrder(secondCourierId, firstOrder);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.takeOrderGroup(
                        firstCourierId,
                        new CourierOrderGroupKey(PRIMARY_CLUSTER, slotFrom, slotTo),
                        3
                )
        );
        assertEquals("Состав группы изменился, обновите страницу", exception.getMessage());

        OrderState firstState = findOrderState(firstOrder);
        OrderState secondState = findOrderState(secondOrder);
        OrderState thirdState = findOrderState(thirdOrder);

        assertEquals("ASSIGNED", firstState.status());
        assertEquals(secondCourierId, firstState.courierId());

        assertEquals("NEW", secondState.status());
        assertNull(secondState.courierId());

        assertEquals("NEW", thirdState.status());
        assertNull(thirdState.courierId());
    }

    @Test
    void assignedOrders_areStillCompletedIndividually() {
        long courierId = registerCourier("79030000131", PRIMARY_CLUSTER);

        long user1 = createUser("79100000131", PRIMARY_CLUSTER, "Новосибирск", "Фрунзе, 1");
        long user2 = createUser("79100000132", PRIMARY_CLUSTER, "Новосибирск", "Фрунзе, 2");
        long user3 = createUser("79100000133", PRIMARY_CLUSTER, "Новосибирск", "Фрунзе, 3");

        OffsetDateTime slotFrom = OffsetDateTime.parse("2026-03-22T10:00:00Z");
        OffsetDateTime slotTo = slotFrom.plusHours(2);

        OrderKey order1 = addOrder(user1, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T07:00:00Z"), slotFrom, slotTo);
        OrderKey order2 = addOrder(user2, "SEPARATE", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T07:05:00Z"), slotFrom, slotTo);
        OrderKey order3 = addOrder(user3, "MIXED", "NEW", PRIMARY_CLUSTER, null,
                OffsetDateTime.parse("2026-03-20T07:10:00Z"), slotFrom, slotTo);

        courierPanelService.takeOrderGroup(
                courierId,
                new CourierOrderGroupKey(PRIMARY_CLUSTER, slotFrom, slotTo),
                3
        );

        long user2DoneBefore = orderInfoService.countDoneOrders(user2);
        long courierPointsBefore = findCourierTotalPoints(courierId);

        courierPanelService.completeOrder(courierId, order2);

        OrderState doneOrderState = findOrderState(order2);
        assertEquals("DONE", doneOrderState.status());
        assertEquals(courierId, doneOrderState.courierId());
        assertNotNull(doneOrderState.completedAt());
        assertEquals(FIXED_NOW.toInstant(), doneOrderState.completedAt().toInstant());

        assertEquals("ASSIGNED", findOrderState(order1).status());
        assertEquals("ASSIGNED", findOrderState(order3).status());

        assertEquals(courierPointsBefore + 20L, findCourierTotalPoints(courierId));
        assertEquals(user2DoneBefore + 1L, orderInfoService.countDoneOrders(user2));

        assertEquals(1L, countEvents(user2, UserActionEventType.ORDER_DONE));
        assertEquals(0L, countEvents(user1, UserActionEventType.ORDER_DONE));
        assertEquals(0L, countEvents(user3, UserActionEventType.ORDER_DONE));
    }

    @Test
    void singleOrderFlowsStillWork() {
        long firstCourierId = registerCourier("79030000141", PRIMARY_CLUSTER);
        long secondCourierId = registerCourier("79030000142", PRIMARY_CLUSTER);
        long userId = createUser("79100000141", PRIMARY_CLUSTER, "Новосибирск", "Державина, 10");

        OffsetDateTime slotFrom = OffsetDateTime.parse("2026-03-22T14:00:00Z");
        OffsetDateTime slotTo = slotFrom.plusHours(2);
        OrderKey order = addOrder(
                userId,
                "MIXED",
                "NEW",
                PRIMARY_CLUSTER,
                null,
                OffsetDateTime.parse("2026-03-20T11:00:00Z"),
                slotFrom,
                slotTo
        );

        courierPanelService.takeOrder(firstCourierId, order);
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courierPanelService.takeOrder(secondCourierId, order)
        );
        assertEquals("Заказ уже взят другим курьером или недоступен", exception.getMessage());

        courierPanelService.completeOrder(firstCourierId, order);
        assertEquals("DONE", findOrderState(order).status());
        assertTrue(findCourierTotalPoints(firstCourierId) >= 20L);
    }

    private void assertAssignedToCourier(OrderKey orderKey, long courierId) {
        OrderState state = findOrderState(orderKey);
        assertEquals("ASSIGNED", state.status());
        assertEquals(courierId, state.courierId());
        assertNotNull(state.assignedAt());
        assertEquals(FIXED_NOW.toInstant(), state.assignedAt().toInstant());
    }

    private long registerCourier(String phone, String postalCode) {
        CourierRegistrationForm form = new CourierRegistrationForm();
        form.setPhone(phone);
        form.setEmail(phone + "@courier.test");
        form.setPassword("secret123");
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
            OffsetDateTime createdAt,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo
    ) {
        OffsetDateTime assignedAt = ("ASSIGNED".equals(status) || "DONE".equals(status))
                ? createdAt.plusMinutes(15)
                : null;
        OffsetDateTime completedAt = "DONE".equals(status)
                ? createdAt.plusHours(1)
                : null;

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

        if ("SEPARATE".equals(type)) {
            addOrderFraction(orderKey, "PAPER");
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
                        rs.getObject("courier_id", Long.class),
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

    private record OrderState(
            String status,
            Long courierId,
            OffsetDateTime assignedAt,
            OffsetDateTime completedAt
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
