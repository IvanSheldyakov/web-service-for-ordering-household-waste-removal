package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourierLoginForm {

    @NotBlank(message = "Телефон обязателен")
    @Pattern(
            regexp = "^\\d{10,15}$",
            message = "Телефон должен содержать 10-15 цифр"
    )
    private String phone;
}
