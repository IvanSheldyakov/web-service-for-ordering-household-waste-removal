create table if not exists event_processor_state
(
    processor_name varchar(64) primary key,
    last_event_id  bigint      not null default 0,
    updated_at     timestamptz not null default now()
);
