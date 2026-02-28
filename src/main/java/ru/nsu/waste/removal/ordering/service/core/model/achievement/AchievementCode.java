package ru.nsu.waste.removal.ordering.service.core.model.achievement;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum AchievementCode {
    ACH_FIRST_ORDER("ACH_FIRST_ORDER"),
    ACH_FIRST_SEPARATE("ACH_FIRST_SEPARATE"),
    ACH_SEPARATE_5("ACH_SEPARATE_5"),
    ACH_LEVEL_UP("ACH_LEVEL_UP"),
    SOC_OPEN_LEADERBOARD("SOC_OPEN_LEADERBOARD"),
    SOC_TOP10_WEEK("SOC_TOP10_WEEK"),
    SOC_TOP3_WEEK("SOC_TOP3_WEEK"),
    SOC_GREEN_HELPER_5("SOC_GREEN_HELPER_5"),
    EXP_OPEN_PROFILE("EXP_OPEN_PROFILE"),
    EXP_CARDS_5("EXP_CARDS_5"),
    EXP_NEW_FRACTIONS_3("EXP_NEW_FRACTIONS_3"),
    EXP_NEW_FRACTIONS_ALL_5("EXP_NEW_FRACTIONS_ALL_5");

    private static final Map<String, AchievementCode> BY_DB_CODE = Arrays.stream(values())
            .collect(Collectors.toMap(AchievementCode::dbCode, Function.identity()));

    private final String dbCode;

    AchievementCode(String dbCode) {
        this.dbCode = dbCode;
    }

    public String dbCode() {
        return dbCode;
    }

    public static Optional<AchievementCode> fromDbCode(String dbCode) {
        return Optional.ofNullable(BY_DB_CODE.get(dbCode));
    }
}
