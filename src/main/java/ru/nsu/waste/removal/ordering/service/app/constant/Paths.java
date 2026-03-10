package ru.nsu.waste.removal.ordering.service.app.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Paths {

    public static final String REGISTRATION = "/registration";

    public static final String COURIER = "/courier";

    public static final String COURIER_ID = "courierId";

    public static final String COURIER_WITH_ID = COURIER + "/{" + COURIER_ID + "}";

    public static final String COURIER_REGISTRATION = COURIER + "/registration";

    public static final String COURIER_LOGIN = COURIER + "/login";

    public static final String COURIER_HOME = "/home";

    public static final String COURIER_PANEL = COURIER_WITH_ID + COURIER_HOME;

    public static final String COURIER_TAKE_ORDER = COURIER_WITH_ID + "/orders/take";

    public static final String COURIER_TAKE_ORDER_GROUP = COURIER_WITH_ID + "/order-groups/take";

    public static final String COURIER_COMPLETE_ORDER = COURIER_WITH_ID + "/orders/complete";

    public static final String QUIZ = "/quiz";

    public static final String REGISTRATION_QUIZ = REGISTRATION + QUIZ;

    public static final String USER_ID = "userId";

    public static final String USER = "/user";

    public static final String USER_WITH_ID = USER + "/{" + USER_ID + "}";

    public static final String USER_LOGIN = USER + "/login";

    public static final String HOME = "/home";

    public static final String ECO_DASHBOARD = "/eco-dashboard";

    public static final String HISTORY = "/history";

    public static final String ORDER_CREATE = "/order/create";

    public static final String USER_HOME = USER_WITH_ID + HOME;

    public static final String USER_ECO_DASHBOARD = USER_WITH_ID + ECO_DASHBOARD;

    public static final String USER_HISTORY = USER_WITH_ID + HISTORY;

    public static final String USER_ORDER_CREATE = USER_WITH_ID + ORDER_CREATE;
}
