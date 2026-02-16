create table if not exists user_info
(
    id             bigserial primary key,
    total_points   bigint        not null default 0,
    current_points bigint        not null default 0,
    habit_strength bigint        not null default 0,
    created_at     timestamptz   not null default now(),
    updated_at     timestamptz   not null default now(),
    type_id        int           not null,
    address_id     bigint        not null,
    person_id      bigint unique not null,
    constraint user_info_type_id_fk foreign key (type_id) references user_type (id),
    constraint user_info_address_id_fk foreign key (address_id) references address (id),
    constraint user_info_person_id_fk foreign key (person_id) references person_info (id)
);

create table if not exists user_type
(
    id   serial primary key,
    name varchar(10) unique not null check ( name in ('ACHIEVER', 'SOCIALIZER', 'EXPLORER') )
);

insert into user_type (name)
values ('ACHIEVER'),
       ('SOCIALIZER'),
       ('EXPLORER')
on conflict (name) do nothing;

create table if not exists achiever_profile
(
    user_id  bigint primary key,
    level_id int not null,
    constraint achiever_profile_user_id_fk
        foreign key (user_id) references user_info (id) on delete cascade,
    constraint achiever_profile_level_id_fk
        foreign key (level_id) references level (id)
);

create table if not exists person_info
(
    id         bigserial primary key,
    phone      bigint unique not null,
    email      varchar(256)  not null,
    name       varchar(256)  not null,
    surname    varchar(256)  not null,
    patronymic varchar(256)
);

create table if not exists address
(
    id               bigint primary key,
    country_code     varchar(2)   not null,
    postal_code      varchar(16)  not null,
    city             varchar(128) not null,
    region           varchar(128) not null,
    detailed_address varchar(256) not null,
    timezone         text         not null -- TODO как заполняем ?
);

create table if not exists level
(
    id                    serial primary key,
    required_total_points int not null unique check ( required_total_points > 0 )
);

insert into level (required_total_points)
values (1000),
       (2000),
       (3000),
       (4000),
       (5000)
on conflict do nothing;

create table if not exists info_card
(
    id          serial primary key,
    title       varchar(128) not null,
    description varchar(256) not null
);

create table if not exists eco_task
(
    id           serial primary key,
    user_type_id int          not null,
    title        varchar(255) not null,
    description  varchar(255) not null,
    points       bigint       not null,
    period       varchar(7)   not null check ( period in ('WEEKLY', 'MONTHLY') ),
    constraint eco_task_user_type_id_fk foreign key (user_type_id) references user_type (id)

);

create table if not exists user_eco_task
(
    id           bigserial primary key,
    user_id      bigint       not null,
    eco_task_id  bigint       not null,
    status       varchar(255) not null default 'ASSIGNED' check ( status in ('ASSIGNED', 'DONE', 'EXPIRED', 'CANCELED') ),
    assigned_at  timestamptz  not null default now(),
    completed_at timestamptz,
    expired_at   timestamptz  not null,
    constraint user_eco_task_user_id_fk foreign key (user_id) references user_info (id),
    constraint user_eco_task_eco_task_id_fk foreign key (eco_task_id) references eco_task (id)
);

create unique index if not exists user_eco_task_one_active_idx
    on user_eco_task (user_id, eco_task_id)
    where status = 'ASSIGNED';

create table if not exists user_action_history
(
    id                bigserial   not null,
    user_id           bigint      not null,
    event_type              varchar     not null, --TODO какие еще типы событий ?
    content           jsonb       not null,
    points_difference bigint      not null default 0,
    created_at        timestamptz not null default now(),
    constraint user_action_history_user_id_fk foreign key (user_id) references user_info (id),
    constraint user_action_history_pk primary key (id, created_at),
    constraint user_action_history_event_type_chk check (
        event_type in (
                       'ORDER_DONE',
                       'LEVEL_UP',
                       'LEADERBOARD_OPENED',
                       'ECO_PROFILE_OPENED',
                       'INFO_CARD_VIEWED',
                       'ACHIEVEMENT_UNLOCKED'
            )
        )
) partition by range (created_at);

create table if not exists user_action_history_2026_02 partition of user_action_history for values from ('2026-02-01') to ('2026-03-01');
create table if not exists user_action_history_2026_03 partition of user_action_history for values from ('2026-03-01') to ('2026-04-01');
create table if not exists user_action_history_2026_04 partition of user_action_history for values from ('2026-04-01') to ('2026-05-01');
create table if not exists user_action_history_2026_05 partition of user_action_history for values from ('2026-05-01') to ('2026-06-01');
create table if not exists user_action_history_2026_06 partition of user_action_history for values from ('2026-06-01') to ('2026-07-01');
create table if not exists user_action_history_2026_07 partition of user_action_history for values from ('2026-07-01') to ('2026-08-01');
create table if not exists user_action_history_2026_08 partition of user_action_history for values from ('2026-08-01') to ('2026-09-01');
create table if not exists user_action_history_2026_09 partition of user_action_history for values from ('2026-09-01') to ('2026-10-01');
create table if not exists user_action_history_2026_10 partition of user_action_history for values from ('2026-10-01') to ('2026-11-01');
create table if not exists user_action_history_2026_11 partition of user_action_history for values from ('2026-11-01') to ('2026-12-01');
create table if not exists user_action_history_2026_12 partition of user_action_history for values from ('2026-12-01') to ('2027-01-01');


create table if not exists courier
(
    id        bigserial primary key,
    person_id bigint not null unique,
    constraint courier_person_id_fk foreign key (person_id) references person_info (id)
);

create table if not exists order_info
(
    id           bigserial   not null,
    user_id      bigint      not null,
    courier_id   bigint,
    created_at   timestamptz not null default now(),
    completed_at timestamptz,
    assigned_at  timestamptz,
    type         varchar(8)  not null check ( type in ('MIXED', 'SEPARATE') ),
    status       varchar(9)  not null check ( status in (('NEW', 'ASSIGNED', 'DONE', 'CANCELLED')) ),
    pickup_from  timestamptz not null,
    pickup_to    timestamptz not null,
    green_chosen boolean     not null default false,
    postal_code  varchar(16) not null,
    constraint order_info_user_id_fk foreign key (user_id) references user_info (id),
    constraint order_info_courier_id_fk foreign key (courier_id) references courier (id),
    constraint order_info_pk primary key (id, created_at)
) partition by range (created_at);

create table if not exists order_info_2026_02 partition of order_info for values from ('2026-02-01') to ('2026-03-01');
create table if not exists order_info_2026_03 partition of order_info for values from ('2026-03-01') to ('2026-04-01');
create table if not exists order_info_2026_04 partition of order_info for values from ('2026-04-01') to ('2026-05-01');
create table if not exists order_info_2026_05 partition of order_info for values from ('2026-05-01') to ('2026-06-01');
create table if not exists order_info_2026_06 partition of order_info for values from ('2026-06-01') to ('2026-07-01');
create table if not exists order_info_2026_07 partition of order_info for values from ('2026-07-01') to ('2026-08-01');
create table if not exists order_info_2026_08 partition of order_info for values from ('2026-08-01') to ('2026-09-01');
create table if not exists order_info_2026_09 partition of order_info for values from ('2026-09-01') to ('2026-10-01');
create table if not exists order_info_2026_10 partition of order_info for values from ('2026-10-01') to ('2026-11-01');
create table if not exists order_info_2026_11 partition of order_info for values from ('2026-11-01') to ('2026-12-01');
create table if not exists order_info_2026_12 partition of order_info for values from ('2026-12-01') to ('2027-01-01');

create table if not exists waste_fraction
(
    id        bigserial primary key,
    type      text unique not null check ( type in ('PLASTIC', 'PAPER', 'GLASS', 'METAL', 'ORGANIC') ),
    name      text        not null,
    is_active boolean     not null default true
);

insert into waste_fraction (type, name)
values ('PLASTIC', 'Пластик'),
       ('PAPER', 'Бумага'),
       ('GLASS', 'Стекло'),
       ('METAL', 'Метал'),
       ('ORGANIC', 'Органика')
on conflict (type) do nothing;

create table if not exists order_waste_fraction
(
    order_id         bigint      not null,
    order_created_at timestamptz not null,
    fraction_id      bigint      not null,
    constraint order_waste_fraction_oder_id_fk foreign key (order_id, order_created_at) references order_info (id, created_at),
    constraint order_waste_fraction_fraction_id_fk foreign key (fraction_id) references waste_fraction (id),
    primary key (order_id, order_created_at, fraction_id)
);

--------------------------- ДОСТИЖЕНИЯ ---------------------------
create table if not exists achievement
(
    id            serial primary key,
    code          varchar(64)  not null unique,
    title         varchar(128) not null,
    description   varchar(256) not null,
    trigger_event varchar(64)  not null,
    user_type_id  int          not null,
    constraint achievement_user_type_id_fk foreign key (user_type_id) references user_type (id)
);

create table if not exists achievement_user
(
    achievement_id int    not null,
    user_id        bigint not null,
    constraint achievement_user_achievement_id_fk foreign key (user_id) references user_info (id),
    constraint achievement_user_user_id_fk foreign key (achievement_id) references achievement (id),
    primary key (achievement_id, user_id)
);

insert into achievement (code, title, description, trigger_event, user_type_id)
values
-- ACHIEVER (прогресс/статус)
('ACH_FIRST_ORDER',
 'Первый вывоз',
 'Выполнить хотя бы один заказ на вывоз отходов.',
 'ORDER_DONE',
 (select id from user_type where name = 'ACHIEVER')),
('ACH_FIRST_SEPARATE',
 'Первый раздельный',
 'Выполнить хотя бы один заказ раздельного вывоза.',
 'ORDER_DONE',
 (select id from user_type where name = 'ACHIEVER')),
('ACH_SEPARATE_5',
 '5 раздельных',
 'Выполнить пять заказов раздельного вывоза.',
 'ORDER_DONE',
 (select id from user_type where name = 'ACHIEVER')),
('ACH_LEVEL_UP',
 'Новый уровень',
 'Достичь нового уровня Achiever (например, уровень >= 2).',
 'LEVEL_UP',
 (select id from user_type where name = 'ACHIEVER')),

-- SOCIALIZER (соревнование/район)
('SOC_OPEN_LEADERBOARD',
 'Заглянул в рейтинг',
 'Открыть страницу рейтинга хотя бы один раз.',
 'LEADERBOARD_OPENED',
 (select id from user_type where name = 'SOCIALIZER')),
('SOC_TOP10_WEEK',
 'Топ-10 района за неделю',
 'Попасть в топ-10 пользователей района по сумме баллов за последние 7 дней.',
 'LEADERBOARD_OPENED',
 (select id from user_type where name = 'SOCIALIZER')),
('SOC_TOP3_WEEK',
 'Топ-3 района за неделю',
 'Попасть в топ-3 пользователей района по сумме баллов за последние 7 дней.',
 'LEADERBOARD_OPENED',
 (select id from user_type where name = 'SOCIALIZER')),
('SOC_GREEN_HELPER_5',
 'Помогаю району',
 'Выбрать зелёные слоты не менее 5 раз в выполненных заказах.',
 'ORDER_DONE',
 (select id from user_type where name = 'SOCIALIZER')),

-- EXPLORER (познание/новизна)
('EXP_OPEN_PROFILE',
 'Открыл эко-профиль',
 'Открыть страницу эко-профиля хотя бы один раз.',
 'ECO_PROFILE_OPENED',
 (select id from user_type where name = 'EXPLORER')),
('EXP_CARDS_5',
 'Эко-читатель: 5 карточек',
 'Просмотреть 5 информационных карточек.',
 'INFO_CARD_VIEWED',
 (select id from user_type where name = 'EXPLORER')),
('EXP_NEW_FRACTIONS_3',
 'Открыл 3 фракции',
 'Использовать 3 разные фракции в выполненных раздельных заказах.',
 'ORDER_DONE',
 (select id from user_type where name = 'EXPLORER')),
('EXP_NEW_FRACTIONS_ALL_5',
 'Пробовал всё: 5 фракций',
 'Использовать 5 разных фракций в выполненных раздельных заказах.',
 'ORDER_DONE',
 (select id from user_type where name = 'EXPLORER'))
on conflict (code) do nothing;

--------------------------- ДОСТИЖЕНИЯ ---------------------------


