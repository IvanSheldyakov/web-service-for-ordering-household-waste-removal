package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@Getter
@Setter
public class CourierOrderActionForm {

    @NotNull(message = "Идентификатор заказа обязателен")
    private Long orderId;

    @NotNull(message = "Время создания заказа обязательно")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime orderCreatedAt;
}
