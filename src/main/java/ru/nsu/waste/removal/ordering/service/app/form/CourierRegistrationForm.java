package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourierRegistrationForm {

    @NotBlank(message = "Телефон обязателен")
    @Pattern(
            regexp = "^\\d{10,15}$",
            message = "Телефон должен содержать 10-15 цифр"
    )
    private String phone;

    @NotBlank(message = "Электронная почта обязательна")
    @Email(message = "Некорректная электронная почта")
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 128, message = "Пароль должен содержать 6-128 символов")
    private String password;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 256, message = "Имя слишком длинное")
    private String name;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 256, message = "Фамилия слишком длинная")
    private String surname;

    @Size(max = 256, message = "Отчество слишком длинное")
    private String patronymic;

    @NotBlank(message = "Почтовый индекс обязателен")
    @Size(max = 16, message = "Почтовый индекс слишком длинный")
    private String postalCode;

    @NotBlank(message = "Часовой пояс обязателен")
    private String timezone;
}
