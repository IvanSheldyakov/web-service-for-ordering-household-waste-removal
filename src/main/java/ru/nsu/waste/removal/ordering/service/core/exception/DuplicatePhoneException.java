package ru.nsu.waste.removal.ordering.service.core.exception;

public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String message, Throwable cause) {
        super(message, cause);
    }
}
