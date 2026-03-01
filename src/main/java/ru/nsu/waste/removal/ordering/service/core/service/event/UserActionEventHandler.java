package ru.nsu.waste.removal.ordering.service.core.service.event;

import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;

public interface UserActionEventHandler {

    boolean supports(UserActionHistoryEvent event);

    void handle(UserActionHistoryEvent event);
}
