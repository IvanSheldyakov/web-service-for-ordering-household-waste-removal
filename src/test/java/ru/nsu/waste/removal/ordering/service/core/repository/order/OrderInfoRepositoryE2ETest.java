package ru.nsu.waste.removal.ordering.service.core.repository.order;

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
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@Tag("e2e")
@Testcontainers
class OrderInfoRepositoryE2ETest {

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
    private OrderInfoRepository orderInfoRepository;

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
    void findActiveOrdersByUserId_returnsOnlyNewAndAssigned_withFractionsAndSorting() {
        long userId = createUser("70000000001", "100001");
        long anotherUserId = createUser("70000000002", "100002");

        OrderRow mixedNew = addOrder(
                userId,
                "MIXED",
                "NEW",
                OffsetDateTime.parse("2026-03-10T09:00:00+00:00"),
                OffsetDateTime.parse("2026-03-10T11:00:00+00:00")
        );

        OrderRow separateAssigned = addOrder(
                userId,
                "SEPARATE",
                "ASSIGNED",
                OffsetDateTime.parse("2026-03-10T12:00:00+00:00"),
                OffsetDateTime.parse("2026-03-10T14:00:00+00:00")
        );

        addOrderFraction(separateAssigned, "PAPER");
        addOrderFraction(separateAssigned, "PLASTIC");

        addOrder(
                userId,
                "SEPARATE",
                "DONE",
                OffsetDateTime.parse("2026-03-10T08:00:00+00:00"),
                OffsetDateTime.parse("2026-03-10T10:00:00+00:00")
        );

        addOrder(
                anotherUserId,
                "MIXED",
                "NEW",
                OffsetDateTime.parse("2026-03-10T07:00:00+00:00"),
                OffsetDateTime.parse("2026-03-10T09:00:00+00:00")
        );

        List<ActiveOrderInfo> orders = orderInfoRepository.findActiveOrdersByUserId(userId, 10);

        assertEquals(2, orders.size());
        assertEquals(List.of(mixedNew.id(), separateAssigned.id()), orders.stream().map(ActiveOrderInfo::orderId).toList());
        assertTrue(orders.stream().allMatch(order -> Set.of("NEW", "ASSIGNED").contains(order.status())));

        ActiveOrderInfo mixedOrder = orders.getFirst();
        assertEquals("MIXED", mixedOrder.type());
        assertTrue(mixedOrder.fractions().isEmpty());

        ActiveOrderInfo separateOrder = orders.get(1);
        assertEquals("SEPARATE", separateOrder.type());
        assertEquals(Set.of("Бумага", "Пластик"), Set.copyOf(separateOrder.fractions()));

        assertFalse(orders.stream().anyMatch(order -> "DONE".equals(order.status())));
    }

    private long createUser(String phone, String postalCode) {
        Long personId = jdbcTemplate.queryForObject(
                """
                        insert into person_info(phone, email, name, surname, patronymic)
                        values (?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                Long.parseLong(phone),
                phone + "@mail.test",
                "Name",
                "Surname",
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
                "City",
                "Region",
                "Street 1",
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

    private OrderRow addOrder(
            long userId,
            String type,
            String status,
            OffsetDateTime pickupFrom,
            OffsetDateTime pickupTo
    ) {
        OrderRow order = jdbcTemplate.queryForObject(
                """
                        insert into order_info(
                            user_id,
                            created_at,
                            type,
                            status,
                            pickup_from,
                            pickup_to,
                            postal_code,
                            cost_points,
                            green_chosen
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        returning id, created_at
                        """,
                (rs, rowNum) -> new OrderRow(
                        rs.getLong("id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                userId,
                OffsetDateTime.parse("2026-03-01T00:00:00+00:00"),
                type,
                status,
                pickupFrom,
                pickupTo,
                "100001",
                100L,
                false
        );

        if (order == null) {
            throw new IllegalStateException("Failed to insert test order");
        }

        return order;
    }

    private void addOrderFraction(OrderRow orderRow, String fractionType) {
        Long fractionId = jdbcTemplate.queryForObject(
                "select id from waste_fraction where type = ?",
                Long.class,
                fractionType
        );

        if (fractionId == null) {
            throw new IllegalStateException("Failed to resolve fraction id");
        }

        jdbcTemplate.update(
                """
                        insert into order_waste_fraction(order_id, order_created_at, fraction_id)
                        values (?, ?, ?)
                        """,
                orderRow.id(),
                orderRow.createdAt(),
                fractionId
        );
    }

    private record OrderRow(
            long id,
            OffsetDateTime createdAt
    ) {
    }
}
