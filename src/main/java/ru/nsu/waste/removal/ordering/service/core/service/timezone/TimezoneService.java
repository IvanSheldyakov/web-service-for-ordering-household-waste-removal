package ru.nsu.waste.removal.ordering.service.core.service.timezone;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TimezoneService {

    private static final List<TimezoneOption> RUSSIAN_TIMEZONES = List.of(
            new TimezoneOption("Europe/Kaliningrad", "Калининград (UTC+02:00)"),
            new TimezoneOption("Europe/Moscow", "Москва (UTC+03:00)"),
            new TimezoneOption("Europe/Samara", "Самара (UTC+04:00)"),
            new TimezoneOption("Asia/Yekaterinburg", "Екатеринбург (UTC+05:00)"),
            new TimezoneOption("Asia/Omsk", "Омск (UTC+06:00)"),
            new TimezoneOption("Asia/Krasnoyarsk", "Красноярск (UTC+07:00)"),
            new TimezoneOption("Asia/Irkutsk", "Иркутск (UTC+08:00)"),
            new TimezoneOption("Asia/Yakutsk", "Якутск (UTC+09:00)"),
            new TimezoneOption("Asia/Vladivostok", "Владивосток (UTC+10:00)"),
            new TimezoneOption("Asia/Magadan", "Магадан (UTC+11:00)"),
            new TimezoneOption("Asia/Kamchatka", "Камчатка (UTC+12:00)")
    );

    private static final Set<String> RUSSIAN_TIMEZONE_SET = RUSSIAN_TIMEZONES.stream()
            .map(TimezoneOption::id)
            .collect(Collectors.toUnmodifiableSet());

    public List<String> getAvailableTimezones() {
        return RUSSIAN_TIMEZONES.stream()
                .map(TimezoneOption::id)
                .toList();
    }

    public List<TimezoneOption> getAvailableTimezoneOptions() {
        return RUSSIAN_TIMEZONES;
    }

    public boolean isSupported(String timezone) {
        return timezone != null && RUSSIAN_TIMEZONE_SET.contains(timezone);
    }

    public record TimezoneOption(
            String id,
            String title
    ) {
    }
}
