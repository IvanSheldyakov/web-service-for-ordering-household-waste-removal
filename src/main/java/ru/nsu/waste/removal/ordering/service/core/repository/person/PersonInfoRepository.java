package ru.nsu.waste.removal.ordering.service.core.repository.person;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

@Repository
@RequiredArgsConstructor
public class PersonInfoRepository {

    private static final String EXISTS_BY_PHONE_QUERY = """
            select exists(
                select 1
                from person_info
                where phone = :phone
            )
            """;

    private static final String ADD_QUERY = """
            insert into person_info(
                                    phone,
                                    email,
                                    name,
                                    surname,
                                    patronymic
                                    )
            values (:phone,
                    :email,
                    :name,
                    :surname,
                    :patronymic
                    )
            returning id
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public boolean existsByPhone(long phone) {
        Boolean exists = namedParameterJdbcTemplate.queryForObject(
                EXISTS_BY_PHONE_QUERY,
                new MapSqlParameterSource(ParameterNames.PHONE, phone),
                Boolean.class
        );
        return exists != null && exists;
    }

    public long add(long phone, String email, String name, String surname, String patronymic) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(ParameterNames.PHONE, phone)
                .addValue(ParameterNames.EMAIL, email)
                .addValue(ParameterNames.NAME, name)
                .addValue(ParameterNames.SURNAME, surname)
                .addValue(ParameterNames.PATRONYMIC, patronymic);

        Long id = namedParameterJdbcTemplate.queryForObject(ADD_QUERY, params, Long.class);

        if (id == null) {

            throw new IllegalStateException("Не удалось получить id для person_info");
        }

        return id;
    }
}
