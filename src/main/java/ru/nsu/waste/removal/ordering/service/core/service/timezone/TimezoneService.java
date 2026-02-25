package ru.nsu.waste.removal.ordering.service.core.service.timezone;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class TimezoneService {

    private static final List<String> RUSSIAN_TIMEZONES = List.of(
            "Europe/Kaliningrad",
            "Europe/Moscow",
            "Europe/Samara",
            "Asia/Yekaterinburg",
            "Asia/Omsk",
            "Asia/Krasnoyarsk",
            "Asia/Irkutsk",
            "Asia/Yakutsk",
            "Asia/Vladivostok",
            "Asia/Magadan",
            "Asia/Kamchatka"
    );

    private static final Set<String> RUSSIAN_TIMEZONE_SET = Set.copyOf(RUSSIAN_TIMEZONES);

    public List<String> getAvailableTimezones() {
        return RUSSIAN_TIMEZONES;
    }

    public boolean isSupported(String timezone) {
        return timezone != null && RUSSIAN_TIMEZONE_SET.contains(timezone);
    }
}
