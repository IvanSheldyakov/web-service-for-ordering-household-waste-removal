package ru.nsu.waste.removal.ordering.service.core.repository.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

@Repository
@RequiredArgsConstructor
public class UserInfoRepository {

    private static final String ADD_USER_INFO_QUERY = """
            insert into user_info(
                                  type_id,
                                  address_id,
                                  person_id
                                  )
            values (
                    :typeId,
                    :addressId,
                    :personId
                    )
            
            returning id
            """;

    private static final String ADD_ACHIEVER_PROFILE_QUERY = """
            insert into achiever_profile(
                                         user_id,
                                         level_id
                                         )
            values (
                    :userId,
                    :levelId
                    )
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long addUserInfo(int typeId, long addressId, long personId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue(ParameterNames.TYPE_ID, typeId)
                .addValue(ParameterNames.ADDRESS_ID, addressId)
                .addValue(ParameterNames.PERSON_ID, personId);

        Long id = namedParameterJdbcTemplate.queryForObject(ADD_USER_INFO_QUERY, params, Long.class);

        if (id == null) {
            throw new IllegalStateException("Не удалось получить id для user_info");
        }

        return id;
    }

    public void addAchieverProfile(long userId, int levelId) {
        namedParameterJdbcTemplate.update(
                ADD_ACHIEVER_PROFILE_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.USER_ID, userId)
                        .addValue(ParameterNames.LEVEL_ID, levelId)
        );
    }
}
