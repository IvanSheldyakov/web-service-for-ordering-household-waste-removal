package ru.nsu.waste.removal.ordering.service.core.repository.person;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ColumnNames;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;
import ru.nsu.waste.removal.ordering.service.core.model.person.PersonCreationData;

import java.util.Optional;

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
                                    password_hash,
                                    name,
                                    surname,
                                    patronymic
                                    )
            values (:phone,
                    :email,
                    :passwordHash,
                    :name,
                    :surname,
                    :patronymic
                    )
            returning id
            """;

    private static final String FIND_PASSWORD_HASH_BY_PHONE_QUERY = """
            select password_hash
            from person_info
            where phone = :phone
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

    public long add(PersonCreationData personCreationData) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(ParameterNames.PHONE, personCreationData.phone())
                .addValue(ParameterNames.EMAIL, personCreationData.email())
                .addValue(ParameterNames.PASSWORD_HASH, personCreationData.password())
                .addValue(ParameterNames.NAME, personCreationData.name())
                .addValue(ParameterNames.SURNAME, personCreationData.surname())
                .addValue(ParameterNames.PATRONYMIC, personCreationData.patronymic());

        Long id = namedParameterJdbcTemplate.queryForObject(ADD_QUERY, params, Long.class);

        if (id == null) {

            throw new IllegalStateException("Не удалось получить id для person_info");
        }

        return id;
    }

    public Optional<String> findPasswordHashByPhone(long phone) {
        return namedParameterJdbcTemplate.query(
                FIND_PASSWORD_HASH_BY_PHONE_QUERY,
                new MapSqlParameterSource(ParameterNames.PHONE, phone),
                (rs, rowNum) -> rs.getString(ColumnNames.PASSWORD_HASH)
        ).stream().findFirst();
    }
}
