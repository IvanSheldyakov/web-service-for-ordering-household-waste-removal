package ru.nsu.waste.removal.ordering.service.app.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateForm {

    @NotBlank(message = "Выберите тип вывоза")
    private String type;

    @NotBlank(message = "Выберите временной слот")
    private String slotKey;

    private List<Long> fractionIds;
}
