package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CourierOrderGroupActionForm {

    @NotBlank(message = "Ключ кластера обязателен")
    private String clusterKey;

    @NotNull(message = "Время начала интервала обязательно")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime pickupFrom;

    @NotNull(message = "Время окончания интервала обязательно")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime pickupTo;

    @NotNull(message = "Ожидаемое количество заказов обязательно")
    @Min(value = 1L, message = "Ожидаемое количество заказов должно быть не меньше 1")
    private Integer expectedOrderCount;
}
