package ru.nsu.waste.removal.ordering.service.app;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Paths {

    public static final String REGISTRATION = "/registration";

    public static final String QUIZ = "/quiz";

    public static final String REGISTRATION_QUIZ = REGISTRATION + QUIZ;
}
