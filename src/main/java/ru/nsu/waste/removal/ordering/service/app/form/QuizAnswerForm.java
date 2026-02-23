package ru.nsu.waste.removal.ordering.service.app.form;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class QuizAnswerForm {

    private Long quizId;
    private Map<Long, Long> answers = new HashMap<>();
}
