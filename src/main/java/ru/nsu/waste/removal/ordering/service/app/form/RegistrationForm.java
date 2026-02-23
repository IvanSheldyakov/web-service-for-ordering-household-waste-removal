package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegistrationForm {

    @NotBlank(message = "Телефон обязателен")
    @Pattern(regexp = "^\\d{10,15}$", message = "Телефон должен содержать 10-15 цифр")
    private String phone;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный email")
    private String email;

    @NotBlank(message = "Имя обязательно")
    @Size(max = 256, message = "Имя слишком длинное")
    private String name;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 256, message = "Фамилия слишком длинная")
    private String surname;

    @Size(max = 256, message = "Отчество слишком длинное")
    private String patronymic;

    @NotBlank(message = "Код страны обязателен")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "Код страны должен состоять из 2 букв")
    private String countryCode;

    @NotBlank(message = "Регион обязателен")
    @Size(max = 128, message = "Регион слишком длинный")
    private String region;

    @NotBlank(message = "Город обязателен")
    @Size(max = 128, message = "Город слишком длинный")
    private String city;

    @NotBlank(message = "Почтовый индекс обязателен")
    @Size(max = 16, message = "Почтовый индекс слишком длинный")
    private String postalCode;

    @NotBlank(message = "Подробный адрес обязателен")
    @Size(max = 256, message = "Адрес слишком длинный")
    private String detailedAddress;

    @NotBlank(message = "Часовой пояс обязателен")
    private String timezone;
}
