package ru.nsu.waste.removal.ordering.service.core.repository.courier;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

import java.util.Optional;
import java.util.stream.Stream;

@Repository
@RequiredArgsConstructor
public class CourierRepository {

    private static final String ADD_QUERY = """
            insert into courier(
                                person_id,
                                total_points,
                                postal_code,
                                timezone
                                )
            values (
                    :personId,
                    0,
                    :postalCode,
                    :timezone
                    )
            returning id
            """;

    private static final String FIND_PROFILE_BY_ID_QUERY = """
            select c.id,
                   c.total_points,
                   c.postal_code,
                   c.timezone,
                   p.name,
                   p.surname,
                   p.patronymic
            from courier c
                     join person_info p on p.id = c.person_id
            where c.id = :courierId
            """;

    private static final String FIND_COURIER_ID_BY_PHONE_QUERY = """
            select c.id
            from courier c
                     join person_info p on p.id = c.person_id
            where p.phone = :phone
            """;

    private static final String FIND_TIMEZONE_BY_COURIER_ID_QUERY = """
            select timezone
            from courier
            where id = :courierId
            """;

    private static final String ADD_POINTS_QUERY = """
            update courier
            set total_points = total_points + :delta
            where id = :courierId
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long add(long personId, String postalCode, String timezone) {
        Long courierId = namedParameterJdbcTemplate.queryForObject(
                ADD_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.PERSON_ID, personId)
                        .addValue(ParameterNames.POSTAL_CODE, postalCode)
                        .addValue(ParameterNames.TIMEZONE, timezone),
                Long.class
        );

        if (courierId == null) {
            throw new IllegalStateException("Не удалось получить id для courier");
        }

        return courierId;
    }

    public Optional<CourierProfileInfo> findProfileById(long courierId) {
        return namedParameterJdbcTemplate.query(
                FIND_PROFILE_BY_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.COURIER_ID, courierId),
                (rs, rowNum) -> new CourierProfileInfo(
                        rs.getLong(ColumnNames.ID),
                        buildFullName(
                                rs.getString(ColumnNames.SURNAME),
                                rs.getString(ColumnNames.NAME),
                                rs.getString(ColumnNames.PATRONYMIC)
                        ),
                        rs.getString(ColumnNames.POSTAL_CODE),
                        rs.getString(ColumnNames.TIMEZONE),
                        rs.getLong(ColumnNames.TOTAL_POINTS)
                )
        ).stream().findFirst();
    }

    public Optional<Long> findCourierIdByPhone(long phone) {
        return namedParameterJdbcTemplate.query(
                FIND_COURIER_ID_BY_PHONE_QUERY,
                new MapSqlParameterSource(ParameterNames.PHONE, phone),
                (rs, rowNum) -> rs.getLong(ColumnNames.ID)
        ).stream().findFirst();
    }

    public Optional<String> findTimezoneByCourierId(long courierId) {
        return namedParameterJdbcTemplate.query(
                FIND_TIMEZONE_BY_COURIER_ID_QUERY,
                new MapSqlParameterSource(ParameterNames.COURIER_ID, courierId),
                (rs, rowNum) -> rs.getString(ColumnNames.TIMEZONE)
        ).stream().findFirst();
    }

    public boolean addPoints(long courierId, long delta) {
        return namedParameterJdbcTemplate.update(
                ADD_POINTS_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.COURIER_ID, courierId)
                        .addValue(ParameterNames.DELTA, delta)
        ) == 1;
    }

    private String buildFullName(String surname, String name, String patronymic) {
        return Stream.of(surname, name, patronymic)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }
}
