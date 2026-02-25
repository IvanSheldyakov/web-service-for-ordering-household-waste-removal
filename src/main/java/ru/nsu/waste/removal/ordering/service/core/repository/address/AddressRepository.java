package ru.nsu.waste.removal.ordering.service.core.repository.address;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;
import ru.nsu.waste.removal.ordering.service.core.model.address.AddressCreationData;

@Repository
@RequiredArgsConstructor
public class AddressRepository {

    private static final String ADD_QUERY = """
            insert into address(
                                country_code,
                                region,
                                city,
                                postal_code,
                                detailed_address,
                                timezone
                                )
            values (
                    :countryCode,
                    :region,
                    :city,
                    :postalCode,
                    :detailedAddress,
                    :timezone
                    )
            returning id
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long add(AddressCreationData addressCreationData) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(ParameterNames.COUNTRY_CODE, addressCreationData.countryCode())
                .addValue(ParameterNames.REGION, addressCreationData.region())
                .addValue(ParameterNames.CITY, addressCreationData.city())
                .addValue(ParameterNames.POSTAL_CODE, addressCreationData.postalCode())
                .addValue(ParameterNames.DETAILED_ADDRESS, addressCreationData.detailedAddress())
                .addValue(ParameterNames.TIMEZONE, addressCreationData.timezone());

        Long id = namedParameterJdbcTemplate.queryForObject(ADD_QUERY, params, Long.class);

        if (id == null) {
            throw new IllegalStateException("Не удалось получить id для address");
        }

        return id;
    }
}
