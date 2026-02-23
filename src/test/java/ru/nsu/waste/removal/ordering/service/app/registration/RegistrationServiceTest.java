package ru.nsu.waste.removal.ordering.service.app.registration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.exception.DuplicatePhoneException;
import ru.nsu.waste.removal.ordering.service.app.form.QuizAnswerForm;
import ru.nsu.waste.removal.ordering.service.app.form.RegistrationForm;
import ru.nsu.waste.removal.ordering.service.app.controller.registration.view.RegistrationResultViewModel;
import ru.nsu.waste.removal.ordering.service.core.service.registration.RegistrationService;

import java.time.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:registrationdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class RegistrationServiceTest {

    private static final String TZ_ALMATY = "Asia/Almaty";

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setupDatabase() {
        dropSchema();
        createSchema();
        seedData();
    }

    @Test
    void happyPath_createsExpectedRecords_andAchieverProfileOnlyForAchiever() {
        RegistrationForm form = validForm("77001234567", TZ_ALMATY);
        QuizAnswerForm answers = answers(1L, 4L, 7L); // achiever by all answers

        RegistrationResultViewModel result = registrationService.register(form, answers);

        assertNotNull(result);
        assertEquals("Достигатель", result.userTypeName());
        assertEquals(0, result.balances().totalPoints());
        assertEquals(0, result.balances().currentPoints());

        assertEquals(1, count("person_info"));
        assertEquals(1, count("address"));
        assertEquals(1, count("user_info"));
        assertEquals(1, count("achiever_profile"));

        int assignedTasks = jdbcTemplate.queryForObject(
                "select count(*) from user_eco_task where user_id = ? and status = 'ASSIGNED'",
                Integer.class,
                result.userId()
        );
        assertTrue(assignedTasks >= 2 && assignedTasks <= 3);
    }

    @Test
    void phoneDuplicate_registrationFails_andNoPartialRowsInserted() {
        RegistrationForm firstForm = validForm("77005554433", TZ_ALMATY);
        QuizAnswerForm firstAnswers = answers(1L, 4L, 7L);
        registrationService.register(firstForm, firstAnswers);

        int personBefore = count("person_info");
        int addressBefore = count("address");
        int userBefore = count("user_info");
        int ecoTaskBefore = count("user_eco_task");

        RegistrationForm secondForm = validForm("77005554433", TZ_ALMATY);
        secondForm.setEmail("another@example.com");
        QuizAnswerForm secondAnswers = answers(3L, 6L, 9L);

        DuplicatePhoneException exception = assertThrows(
                DuplicatePhoneException.class,
                () -> registrationService.register(secondForm, secondAnswers)
        );
        assertEquals("Телефон уже зарегистрирован", exception.getMessage());

        assertEquals(personBefore, count("person_info"));
        assertEquals(addressBefore, count("address"));
        assertEquals(userBefore, count("user_info"));
        assertEquals(ecoTaskBefore, count("user_eco_task"));
    }

    @Test
    void tie_resolvedByTieBreakQuestion() {
        RegistrationForm form = validForm("77001112233", TZ_ALMATY);
        QuizAnswerForm tieAnswers = answers(1L, 5L, 9L); // tie score, tiebreak -> explorer

        RegistrationResultViewModel result = registrationService.register(form, tieAnswers);

        assertEquals("Исследователь", result.userTypeName());
        assertEquals(0, count("achiever_profile"));
    }

    @Test
    void expiredAt_weeklyAndMonthly_areCalculatedInUserTimezone() {
        RegistrationForm form = validForm("77009998877", TZ_ALMATY);
        QuizAnswerForm explorerAnswers = answers(3L, 6L, 9L); // explorer

        RegistrationResultViewModel result = registrationService.register(form, explorerAnswers);
        Long userId = result.userId();

        OffsetDateTime weeklyExpired = jdbcTemplate.queryForObject(
                "select uet.expired_at from user_eco_task uet " +
                        "join eco_task et on et.id = uet.eco_task_id " +
                        "where uet.user_id = ? and et.code = 'EXP_WEEKLY_1'",
                OffsetDateTime.class,
                userId
        );
        OffsetDateTime monthlyExpired = jdbcTemplate.queryForObject(
                "select uet.expired_at from user_eco_task uet " +
                        "join eco_task et on et.id = uet.eco_task_id " +
                        "where uet.user_id = ? and et.code = 'EXP_MONTHLY_1'",
                OffsetDateTime.class,
                userId
        );

        ZoneId almaty = ZoneId.of(TZ_ALMATY);
        OffsetDateTime expectedWeekly = ZonedDateTime.of(
                2026, 2, 22, 23, 59, 59, 0, almaty
        ).toOffsetDateTime();
        OffsetDateTime expectedMonthly = ZonedDateTime.of(
                2026, 2, 28, 23, 59, 59, 0, almaty
        ).toOffsetDateTime();

        assertNotNull(weeklyExpired);
        assertNotNull(monthlyExpired);
        assertEquals(expectedWeekly.toInstant(), weeklyExpired.toInstant());
        assertEquals(expectedMonthly.toInstant(), monthlyExpired.toInstant());
    }

    private RegistrationForm validForm(String phone, String timezone) {
        RegistrationForm form = new RegistrationForm();
        form.setPhone(phone);
        form.setEmail("user@example.com");
        form.setName("Ivan");
        form.setSurname("Petrov");
        form.setPatronymic("Sergeevich");
        form.setCountryCode("KZ");
        form.setRegion("Almaty Region");
        form.setCity("Almaty");
        form.setPostalCode("050000");
        form.setDetailedAddress("Abay 10");
        form.setTimezone(timezone);
        return form;
    }

    private QuizAnswerForm answers(Long q1OptionId, Long q2OptionId, Long q3OptionId) {
        QuizAnswerForm form = new QuizAnswerForm();
        form.setQuizId(1L);
        form.setAnswers(Map.of(
                1L, q1OptionId,
                2L, q2OptionId,
                3L, q3OptionId
        ));
        return form;
    }

    private int count(String table) {
        Integer count = jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
        return count == null ? 0 : count;
    }

    private void dropSchema() {
        jdbcTemplate.execute("drop table if exists user_eco_task");
        jdbcTemplate.execute("drop table if exists eco_task");
        jdbcTemplate.execute("drop table if exists achievement");
        jdbcTemplate.execute("drop table if exists info_card");
        jdbcTemplate.execute("drop table if exists achiever_profile");
        jdbcTemplate.execute("drop table if exists user_info");
        jdbcTemplate.execute("drop table if exists level");
        jdbcTemplate.execute("drop table if exists address");
        jdbcTemplate.execute("drop table if exists person_info");
        jdbcTemplate.execute("drop table if exists registration_quiz_option");
        jdbcTemplate.execute("drop table if exists registration_quiz_question");
        jdbcTemplate.execute("drop table if exists registration_quiz");
        jdbcTemplate.execute("drop table if exists user_type");
    }

    private void createSchema() {
        jdbcTemplate.execute("""
                create table user_type(
                    id int generated by default as identity primary key,
                    name varchar(10) unique not null
                )
                """);
        jdbcTemplate.execute("""
                create table person_info(
                    id bigint generated by default as identity primary key,
                    phone bigint unique not null,
                    email varchar(256) not null,
                    name varchar(256) not null,
                    surname varchar(256) not null,
                    patronymic varchar(256)
                )
                """);
        jdbcTemplate.execute("""
                create table address(
                    id bigint generated by default as identity primary key,
                    country_code varchar(2) not null,
                    postal_code varchar(16) not null,
                    city varchar(128) not null,
                    region varchar(128) not null,
                    detailed_address varchar(256) not null,
                    timezone varchar(64) not null
                )
                """);
        jdbcTemplate.execute("""
                create table level(
                    id int generated by default as identity primary key,
                    required_total_points int not null
                )
                """);
        jdbcTemplate.execute("""
                create table user_info(
                    id bigint generated by default as identity primary key,
                    total_points bigint not null default 0,
                    current_points bigint not null default 0,
                    habit_strength bigint not null default 0,
                    created_at timestamp with time zone not null default current_timestamp,
                    updated_at timestamp with time zone not null default current_timestamp,
                    type_id int not null,
                    address_id bigint not null,
                    person_id bigint unique not null
                )
                """);
        jdbcTemplate.execute("""
                create table achiever_profile(
                    user_id bigint primary key,
                    level_id int not null
                )
                """);
        jdbcTemplate.execute("""
                create table eco_task(
                    id int generated by default as identity primary key,
                    code varchar(64) not null unique,
                    trigger_event varchar(64) not null,
                    rule varchar(2048) not null,
                    user_type_id int not null,
                    title varchar(255) not null,
                    description varchar(255) not null,
                    points bigint not null,
                    period varchar(7) not null,
                    is_active boolean not null default true
                )
                """);
        jdbcTemplate.execute("""
                create table user_eco_task(
                    id bigint generated by default as identity primary key,
                    user_id bigint not null,
                    eco_task_id int not null,
                    status varchar(32) not null default 'ASSIGNED',
                    assigned_at timestamp with time zone not null default current_timestamp,
                    completed_at timestamp with time zone,
                    expired_at timestamp with time zone not null
                )
                """);
        jdbcTemplate.execute("""
                create table registration_quiz(
                    id int generated by default as identity primary key,
                    code varchar(64) not null,
                    version int not null,
                    is_active boolean not null default false
                )
                """);
        jdbcTemplate.execute("""
                create table registration_quiz_question(
                    id int generated by default as identity primary key,
                    quiz_id int not null,
                    code varchar(64) not null,
                    ord int not null,
                    text varchar(512) not null,
                    is_active boolean not null default true,
                    is_tiebreak boolean not null default false
                )
                """);
        jdbcTemplate.execute("""
                create table registration_quiz_option(
                    id int generated by default as identity primary key,
                    question_id int not null,
                    ord int not null,
                    text varchar(512) not null,
                    user_type_id int not null,
                    score int not null,
                    is_active boolean not null default true
                )
                """);
        jdbcTemplate.execute("""
                create table info_card(
                    id int generated by default as identity primary key,
                    title varchar(128) not null,
                    description varchar(256) not null
                )
                """);
        jdbcTemplate.execute("""
                create table achievement(
                    id int generated by default as identity primary key,
                    code varchar(64) not null unique,
                    title varchar(128) not null,
                    description varchar(256) not null,
                    trigger_event varchar(64) not null,
                    user_type_id int not null
                )
                """);
    }

    private void seedData() {
        jdbcTemplate.update("insert into user_type(id, name) values (1, 'ACHIEVER')");
        jdbcTemplate.update("insert into user_type(id, name) values (2, 'SOCIALIZER')");
        jdbcTemplate.update("insert into user_type(id, name) values (3, 'EXPLORER')");

        jdbcTemplate.update("insert into level(id, required_total_points) values (1, 1000)");
        jdbcTemplate.update("insert into level(id, required_total_points) values (2, 2000)");

        jdbcTemplate.update("""
                insert into registration_quiz(id, code, version, is_active)
                values (1, 'REGISTRATION_HEXAD_LITE', 1, true)
                """);

        jdbcTemplate.update("""
                insert into registration_quiz_question(id, quiz_id, code, ord, text, is_active, is_tiebreak)
                values (1, 1, 'Q1', 1, 'Q1 text', true, false)
                """);
        jdbcTemplate.update("""
                insert into registration_quiz_question(id, quiz_id, code, ord, text, is_active, is_tiebreak)
                values (2, 1, 'Q2', 2, 'Q2 text', true, false)
                """);
        jdbcTemplate.update("""
                insert into registration_quiz_question(id, quiz_id, code, ord, text, is_active, is_tiebreak)
                values (3, 1, 'Q3_TIE', 3, 'Q3 text', true, true)
                """);

        insertOption(1, 1, 1, "Q1 Ach", 1, 2);
        insertOption(2, 1, 2, "Q1 Soc", 2, 2);
        insertOption(3, 1, 3, "Q1 Exp", 3, 2);
        insertOption(4, 2, 1, "Q2 Ach", 1, 2);
        insertOption(5, 2, 2, "Q2 Soc", 2, 2);
        insertOption(6, 2, 3, "Q2 Exp", 3, 2);
        insertOption(7, 3, 1, "Q3 Ach", 1, 2);
        insertOption(8, 3, 2, "Q3 Soc", 2, 2);
        insertOption(9, 3, 3, "Q3 Exp", 3, 2);

        insertEcoTask(1, "ACH_WEEKLY_1", 1, "WEEKLY");
        insertEcoTask(2, "ACH_MONTHLY_1", 1, "MONTHLY");
        insertEcoTask(3, "ACH_WEEKLY_2", 1, "WEEKLY");
        insertEcoTask(4, "SOC_WEEKLY_1", 2, "WEEKLY");
        insertEcoTask(5, "EXP_WEEKLY_1", 3, "WEEKLY");
        insertEcoTask(6, "EXP_MONTHLY_1", 3, "MONTHLY");

        jdbcTemplate.update("""
                insert into achievement(id, code, title, description, trigger_event, user_type_id)
                values (1, 'A1', 'Ach 1', 'Achiever achievement', 'ORDER_DONE', 1)
                """);
        jdbcTemplate.update("""
                insert into achievement(id, code, title, description, trigger_event, user_type_id)
                values (2, 'S1', 'Soc 1', 'Socializer achievement', 'ORDER_DONE', 2)
                """);
        jdbcTemplate.update("""
                insert into achievement(id, code, title, description, trigger_event, user_type_id)
                values (3, 'E1', 'Exp 1', 'Explorer achievement', 'ORDER_DONE', 3)
                """);

        jdbcTemplate.update("insert into info_card(id, title, description) values (1, 'Card1', 'Desc1')");
        jdbcTemplate.update("insert into info_card(id, title, description) values (2, 'Card2', 'Desc2')");
        jdbcTemplate.update("insert into info_card(id, title, description) values (3, 'Card3', 'Desc3')");
    }

    private void insertOption(int id, int questionId, int ord, String text, int userTypeId, int score) {
        jdbcTemplate.update("""
                insert into registration_quiz_option(id, question_id, ord, text, user_type_id, score, is_active)
                values (?, ?, ?, ?, ?, ?, true)
                """, id, questionId, ord, text, userTypeId, score);
    }

    private void insertEcoTask(int id, String code, int userTypeId, String period) {
        jdbcTemplate.update("""
                insert into eco_task(id, code, trigger_event, rule, user_type_id, title, description, points, period, is_active)
                values (?, ?, 'ORDER_DONE', '{"type":"ORDER_COUNT","target":1}', ?, ?, ?, 100, ?, true)
                """, id, code, userTypeId, code + " title", code + " desc", period);
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-02-18T10:15:30Z"), ZoneOffset.UTC);
        }
    }
}
