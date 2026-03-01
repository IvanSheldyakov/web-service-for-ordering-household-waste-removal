package ru.nsu.waste.removal.ordering.service.core.service.reward;

import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionEventType;
import ru.nsu.waste.removal.ordering.service.core.model.event.UserActionHistoryEvent;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserRewardState;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserInfoRepository;
import ru.nsu.waste.removal.ordering.service.core.service.event.UserActionEventHandler;

@Service
@Order(10)
@RequiredArgsConstructor
public class UserRewardEventService implements UserActionEventHandler {

    private final UserInfoRepository userInfoRepository;

    @Override
    public boolean supports(UserActionHistoryEvent event) {
        return event.eventType() == UserActionEventType.ECO_TASK_COMPLETED && event.pointsDifference() > 0L;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(UserActionHistoryEvent event) {
        UserRewardState rewardState = userInfoRepository.findRewardStateForUpdate(event.userId())
                .orElseThrow(() -> new IllegalStateException(
                        "User with id = %s is not found".formatted(event.userId())
                ));

        long newTotalPoints = safeAdd(rewardState.totalPoints(), event.pointsDifference());
        long newCurrentPoints = safeAdd(rewardState.currentPoints(), event.pointsDifference());

        userInfoRepository.updateRewardState(
                event.userId(),
                newTotalPoints,
                newCurrentPoints,
                rewardState.habitStrength()
        );
    }

    private static long safeAdd(long a, long b) {
        long result = a + b;
        if (((a ^ result) & (b ^ result)) < 0) {
            throw new ArithmeticException("long overflow during points update");
        }
        return result;
    }
}
