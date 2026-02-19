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

create table if not exists user_info
(
    id             bigserial primary key,
    total_points   bigint        not null default 0 check (total_points >= 0),
    current_points bigint        not null default 0 check (current_points >= 0),
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

create table if not exists achiever_profile
(
    user_id  bigint primary key,
    level_id int not null,
    constraint achiever_profile_user_id_fk
        foreign key (user_id) references user_info (id) on delete cascade,
    constraint achiever_profile_level_id_fk
        foreign key (level_id) references level (id)
);

create table if not exists courier
(
    id           bigserial primary key,
    person_id    bigint not null unique,
    total_points bigint not null default 0 check (total_points >= 0),
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
    status       varchar(9)  not null check ( status in ('NEW', 'ASSIGNED', 'DONE', 'CANCELLED') ),
    pickup_from  timestamptz not null,
    pickup_to    timestamptz not null,
    green_chosen boolean     not null default false,
    postal_code  varchar(16) not null,
    cost_points  bigint      not null check (cost_points > 0),
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

--------------------------- ИНФО КАРТОЧКИ ---------------------------

create table if not exists info_card
(
    id          serial primary key,
    title       varchar(128) not null,
    description varchar(256) not null
);

insert into info_card (title, description)
values ('Сортировка с нуля',
        'Начни с 2 фракций: бумага и пластик. Когда это станет привычкой — добавь стекло/металл. Маленькие шаги дают стабильный результат.'),
       ('Пластик: чистый и сухой',
        'Большинство пунктов приёма просит пластик без остатков еды и жидкости. Ополосни и высуши — так повышается шанс, что его реально переработают.'),
       ('Пластик: смотри маркировку',
        'Маркировка на упаковке помогает понять, как её обычно принимают. Если сомневаешься — лучше смешанный вывоз, чем “заразить” партию вторсырья.'),
       ('Бумага не любит жир',
        'Салфетки, коробки из-под пиццы и бумага с жиром чаще всего не подходят для переработки. Чистая сухая бумага — самый “надёжный” вклад.'),
       ('Стекло: крышки отдельно',
        'Стеклянную тару обычно принимают отдельно от крышек и дозаторов. Сними крышку — и сортировка станет точнее.'),
       ('Почему стекло ценное',
        'Стекло можно перерабатывать многократно без существенной потери качества. Поэтому чистая стеклотара — одна из самых понятных фракций.'),
       ('Металл: сплющи упаковку',
        'Сминай банки и бутылки перед сдачей: так помещается больше, а вывоз и хранение становятся эффективнее.'),
       ('Опасные отходы — отдельно',
        'Батарейки, лампы и электроника не должны попадать в общий пакет: там могут быть токсичные компоненты. Лучше сдавать их в спецпункты.'),
       ('Зелёный слот — это про логистику',
        'Если выбрать “зелёный” слот, заказы в районе легче объединить в один маршрут. Меньше поездок — меньше топлива и выбросов.'),
       ('Привычка держится на повторениях',
        'Экопривычки формируются не силой воли, а рутиной. Упростишь шаги — увеличишь регулярность.')
on conflict do nothing;
--------------------------- ИНФО КАРТОЧКИ ---------------------------

--------------------------- ФРАКЦИИ ---------------------------
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
--------------------------- ФРАКЦИИ ---------------------------

--------------------------- СОБЫТИЯ ---------------------------

create table if not exists user_action_history
(
    id                bigserial   not null,
    user_id           bigint      not null,
    event_type        varchar     not null,
    content           jsonb       not null,
    points_difference bigint      not null default 0,
    created_at        timestamptz not null default now(),
    constraint user_action_history_user_id_fk foreign key (user_id) references user_info (id),
    constraint user_action_history_pk primary key (id, created_at),
    constraint user_action_history_event_type_chk check (
        event_type in (
                       'ORDER_DONE',
                       'ORDER_CREATED',
                       'SEPARATE_CHOSEN',
                       'GREEN_SLOT_CHOSEN',
                       'LEVEL_UP',
                       'LEADERBOARD_OPENED',
                       'ECO_PROFILE_OPENED',
                       'INFO_CARD_VIEWED',
                       'ACHIEVEMENT_UNLOCKED',
                       'ECO_TASK_COMPLETED'
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
--------------------------- СОБЫТИЯ ---------------------------

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
    constraint achievement_user_user_id_fk foreign key (user_id) references user_info (id),
    constraint achievement_user_achievement_id_fk foreign key (achievement_id) references achievement (id),
    primary key (achievement_id, user_id)
);

insert into achievement (code, title, description, trigger_event, user_type_id)
values ('ACH_FIRST_ORDER',
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

--------------------------- ЭКО-ЗАДАНИЯ ---------------------------
create table if not exists eco_task
(
    id            serial primary key,
    code          varchar(64)  not null unique,
    trigger_event varchar(64)  not null,
    rule          jsonb        not null,
    user_type_id  int          not null,
    title         varchar(255) not null,
    description   varchar(255) not null,
    points        bigint       not null,
    period        varchar(7)   not null check ( period in ('WEEKLY', 'MONTHLY') ),
    is_active     boolean      not null default true,
    constraint eco_task_user_type_id_fk foreign key (user_type_id) references user_type (id),
    constraint eco_task_rule_chk check (
        rule ? 'type'
            and rule ? 'target'
            and jsonb_typeof(rule -> 'target') = 'number'
            and (rule ->> 'type') in ('ORDER_COUNT', 'DISTINCT_FRACTIONS', 'ACTION_COUNT')
            and (not (rule ? 'filters') or jsonb_typeof(rule -> 'filters') = 'object')
        )

);

create table if not exists user_eco_task
(
    id           bigserial primary key,
    user_id      bigint       not null,
    eco_task_id  int          not null,
    status       varchar(255) not null default 'ASSIGNED' check ( status in ('ASSIGNED', 'DONE', 'EXPIRED', 'CANCELLED') ),
    assigned_at  timestamptz  not null default now(),
    completed_at timestamptz,
    expired_at   timestamptz  not null,
    constraint user_eco_task_user_id_fk foreign key (user_id) references user_info (id),
    constraint user_eco_task_eco_task_id_fk foreign key (eco_task_id) references eco_task (id)
);

create unique index if not exists user_eco_task_one_active_idx
    on user_eco_task (user_id, eco_task_id)
    where status = 'ASSIGNED';

insert into eco_task (code, user_type_id, title, description, points, period, trigger_event, rule)
values ('TASK_ACH_SEPARATE_5_WEEK',
        (select id from user_type where name = 'ACHIEVER'),
        '5 раздельных за неделю',
        'Выполни 5 заказов раздельного вывоза за неделю.',
        100,
        'WEEKLY',
        'ORDER_DONE',
        '{
          "type": "ORDER_COUNT",
          "filters": {
            "type": "SEPARATE",
            "status": "DONE"
          },
          "target": 5
        }'::jsonb),

       ('TASK_SOC_GREEN_3_WEEK',
        (select id from user_type where name = 'SOCIALIZER'),
        '3 зелёных слота за неделю',
        'Выбери зелёный слот в 3 выполненных заказах за неделю.',
        50,
        'WEEKLY',
        'ORDER_DONE',
        '{
          "type": "ORDER_COUNT",
          "filters": {
            "green_chosen": true,
            "status": "DONE"
          },
          "target": 3
        }'::jsonb),

       ('TASK_EXP_FRACTIONS_3_MONTH',
        (select id from user_type where name = 'EXPLORER'),
        'Открой 3 фракции',
        'Используй 3 разные фракции в выполненных раздельных заказах за месяц.',
        50,
        'MONTHLY',
        'ORDER_DONE',
        '{
          "type": "DISTINCT_FRACTIONS",
          "filters": {
            "type": "SEPARATE",
            "status": "DONE"
          },
          "target": 3
        }'::jsonb),

       ('TASK_EXP_OPEN_PROFILE_WEEK',
        (select id from user_type where name = 'EXPLORER'),
        'Загляни в эко-профиль',
        'Открой страницу эко-профиля хотя бы один раз за неделю.',
        30,
        'WEEKLY',
        'ECO_PROFILE_OPENED',
        '{
          "type": "ACTION_COUNT",
          "target": 1
        }'::jsonb)
on conflict (code) do nothing;
--------------------------- ЭКО-ЗАДАНИЯ ---------------------------

--------------------------- ОПРОСНИК ПРИ РЕГИСТРАЦИИ ---------------------------
create table if not exists registration_quiz
(
    id        serial primary key,
    code      varchar(64) not null,
    version   int         not null check (version > 0),
    is_active boolean     not null default false,
    unique (code, version)
);

create table if not exists registration_quiz_question
(
    id          serial primary key,
    quiz_id     int          not null,
    code        varchar(64)  not null,
    ord         int          not null check (ord > 0),
    text        varchar(512) not null,
    is_active   boolean      not null default true,
    is_tiebreak boolean      not null default false,
    constraint registration_quiz_question_quiz_id_fk
        foreign key (quiz_id) references registration_quiz (id) on delete cascade,
    unique (quiz_id, code),
    unique (quiz_id, ord)
);

create table if not exists registration_quiz_option
(
    id           serial primary key,
    question_id  int          not null,
    ord          int          not null check (ord > 0),
    text         varchar(512) not null,
    user_type_id int          not null,
    score        int          not null default 2 check (score > 0),
    is_active    boolean      not null default true,
    constraint registration_quiz_option_question_id_fk
        foreign key (question_id) references registration_quiz_question (id) on delete cascade,
    constraint registration_quiz_option_user_type_id_fk
        foreign key (user_type_id) references user_type (id),
    unique (question_id, ord)
);

create index if not exists registration_quiz_question_quiz_idx
    on registration_quiz_question (quiz_id);

create index if not exists registration_quiz_option_question_idx
    on registration_quiz_option (question_id);

insert into registration_quiz (code, version, is_active)
values ('REGISTRATION_HEXAD_LITE', 1, true)
on conflict (code, version) do update
    set is_active = excluded.is_active;

insert into registration_quiz_question (quiz_id, code, ord, text, is_tiebreak)
values (1,
        'Q1_MAIN_MOTIVATION', 1,
        'Что вас больше всего мотивирует продолжать пользоваться сервисом?', false),

       (1,
        'Q2_TASK_STYLE', 2,
        'Какие задания вам интереснее выполнять?', false),

       (1,
        'Q3_AFTER_ACTION', 3,
        'После действия (заказ/зелёный слот/задание) что вам приятнее увидеть?', false),

       (1,
        'Q4_HOME_FOCUS', 4,
        'Что вы хотите видеть на главной странице в первую очередь?', false),

       (1,
        'Q5_COMPETITION', 5,
        'Как вы относитесь к соревнованию?', false),

       (1,
        'Q6_DECISION_STYLE', 6,
        'Когда вы выбираете, как действовать, что вам ближе?', false),

       (1,
        'Q7_TIEBREAK', 7,
        'Если выбрать только одно, что важнее всего?', true)
on conflict (quiz_id, code) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (1,
        1,
        'Видеть личный прогресс: цели, уровни, “ещё чуть-чуть до следующего шага”.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (1,
        2,
        'Чувствовать себя частью района: сравнивать результаты и вклад “соседей”.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (1,
        3,
        'Узнавать новое: советы и объяснения, как правильно сортировать.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (2,
        1,
        'Чёткие цели: “сделай N раз” и отметка “выполнено”.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (2,
        2,
        'Челленджи с другими: “сделаем вместе / посмотрим, кто активнее”.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (2,
        3,
        'Задания-эксперименты: попробовать новый способ сортировки и понять, как это работает.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (3,
        1,
        'Прогресс засчитан: +баллы, шаг к уровню/цели.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (3,
        2,
        'Это повлияло на рейтинг/позицию в районе.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (3,
        3,
        'Короткое объяснение: почему это полезно и что это даёт.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (4,
        1,
        'Мой уровень и прогресс до следующего уровня.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (4,
        2,
        'Моё место в рейтинге района (за неделю/месяц).',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (4,
        3,
        '“Совет/факт дня” — короткая полезная карточка.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (5,
        1,
        'Мне важнее “соревноваться с собой” — улучшать личный результат.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (5,
        2,
        'Мне нравится дружеское соревнование с другими людьми.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (5,
        3,
        'Соревнование не важно — важнее разобраться и делать правильно.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (6,
        1,
        'Опираться на цифры и прогресс (баллы, уровни, цели).',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (6,
        2,
        'Опираться на пример/результаты других людей рядом.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (6,
        3,
        'Сначала понять “почему так”, а потом действовать.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

insert into registration_quiz_option (question_id, ord, text, user_type_id, score)
values (7,
        1,
        'Личный прогресс и достижение целей.',
        (select id from user_type where name = 'ACHIEVER'),
        2),
       (7,
        2,
        'Сообщество и сравнение результатов.',
        (select id from user_type where name = 'SOCIALIZER'),
        2),
       (7,
        3,
        'Понимание и новые знания.',
        (select id from user_type where name = 'EXPLORER'),
        2)
on conflict (question_id, ord) do nothing;

--------------------------- ОПРОСНИК ПРИ РЕГИСТРАЦИИ ---------------------------
