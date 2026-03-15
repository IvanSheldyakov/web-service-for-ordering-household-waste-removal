create table if not exists sorting_regularity_window
(
    id           bigserial primary key,
    user_id      bigint       not null,
    window_start timestamptz  not null,
    window_end   timestamptz  not null,
    status       varchar(16)  not null check ( status in ('CONFIRMED', 'MISSED') ),
    created_at   timestamptz  not null default now(),
    constraint sorting_regularity_window_user_id_fk
        foreign key (user_id) references user_info (id) on delete cascade,
    constraint sorting_regularity_window_unique_window
        unique (user_id, window_start)
);

create index if not exists ix_sorting_regularity_window_start
    on sorting_regularity_window (window_start);

alter table if exists user_action_history
    drop constraint if exists user_action_history_event_type_chk;

alter table if exists user_action_history
    add constraint user_action_history_event_type_chk check (
        event_type in (
            'ORDER_DONE',
            'ORDER_CREATED',
            'ORDER_PAID_WITH_POINTS',
            'SEPARATE_CHOSEN',
            'GREEN_SLOT_CHOSEN',
            'LEVEL_UP',
            'LEADERBOARD_OPENED',
            'ECO_PROFILE_OPENED',
            'INFO_CARD_VIEWED',
            'ACHIEVEMENT_UNLOCKED',
            'ECO_TASK_COMPLETED',
            'ECO_TASK_REWARD_REQUEST',
            'SORTING_REGULARITY_CONFIRMED',
            'SORTING_REGULARITY_MISSED'
        )
    );
