package ru.nsu.waste.removal.ordering.service.core.repository.history;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.nsu.waste.removal.ordering.service.core.repository.constant.ParameterNames;

@Repository
@RequiredArgsConstructor
public class EventProcessorStateRepository {

    private static final String FIND_LAST_EVENT_ID_FOR_UPDATE_QUERY = """
            select last_event_id
            from event_processor_state
            where processor_name = :name
            for update
            """;

    private static final String INIT_PROCESSOR_STATE_QUERY = """
            insert into event_processor_state(
                                              processor_name,
                                              last_event_id,
                                              updated_at
                                              )
            values (
                    :name,
                    0,
                    now()
                    )
            on conflict (processor_name) do nothing
            """;

    private static final String UPDATE_LAST_EVENT_ID_QUERY = """
            update event_processor_state
            set last_event_id = :id,
                updated_at = now()
            where processor_name = :name
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public long findLastEventIdForUpdate(String processorName) {
        Long value = namedParameterJdbcTemplate.queryForObject(
                FIND_LAST_EVENT_ID_FOR_UPDATE_QUERY,
                new MapSqlParameterSource(ParameterNames.NAME, processorName),
                Long.class
        );
        return value == null ? 0L : value;
    }

    public void initProcessorStateIfAbsent(String processorName) {
        namedParameterJdbcTemplate.update(
                INIT_PROCESSOR_STATE_QUERY,
                new MapSqlParameterSource(ParameterNames.NAME, processorName)
        );
    }

    public void updateLastEventId(String processorName, long lastEventId) {
        namedParameterJdbcTemplate.update(
                UPDATE_LAST_EVENT_ID_QUERY,
                new MapSqlParameterSource()
                        .addValue(ParameterNames.NAME, processorName)
                        .addValue(ParameterNames.ID, lastEventId)
        );
    }
}
