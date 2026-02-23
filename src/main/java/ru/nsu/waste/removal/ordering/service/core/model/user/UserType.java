package ru.nsu.waste.removal.ordering.service.core.model.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Locale;

@Getter
@RequiredArgsConstructor
public enum UserType {
    ACHIEVER(1, "\u0414\u043e\u0441\u0442\u0438\u0433\u0430\u0442\u0435\u043b\u044c"),
    SOCIALIZER(2, "\u0421\u043e\u0446\u0438\u0430\u043b\u0438\u0437\u0430\u0442\u043e\u0440"),
    EXPLORER(3, "\u0418\u0441\u0441\u043b\u0435\u0434\u043e\u0432\u0430\u0442\u0435\u043b\u044c");

    private final int id;
    private final String russianName;

    public static UserType fromDbName(String value) {
        return UserType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public static UserType fromId(int id) {
        for (UserType type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("UserType с id = %s не существует".formatted(id));
    }
}
