package ru.nsu.waste.removal.ordering.service.app.controller.registration.exception;

public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String message, Throwable cause) {
        super(message, cause);
    }
}
