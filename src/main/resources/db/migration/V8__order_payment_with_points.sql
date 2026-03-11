alter table if exists order_info
    add column if not exists payment_status varchar(32);

update order_info
set payment_status = 'UNPAID'
where payment_status is null;

alter table if exists order_info
    alter column payment_status set default 'UNPAID';

alter table if exists order_info
    alter column payment_status set not null;

alter table if exists order_info
    add column if not exists paid_at timestamptz;

alter table if exists order_info
    drop constraint if exists order_info_payment_status_chk;

alter table if exists order_info
    add constraint order_info_payment_status_chk
        check (payment_status in ('UNPAID', 'PAID_WITH_POINTS'));

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
            'ECO_TASK_COMPLETED'
        )
    );

create index if not exists ix_uah_user_event_type_created_at
    on user_action_history (user_id, event_type, created_at desc);

