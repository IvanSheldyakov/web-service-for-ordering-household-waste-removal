package ru.nsu.waste.removal.ordering.service.core.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.repository.order.OrderInfoRepository;

@Service
@RequiredArgsConstructor
public class OrderInfoService {

    private final OrderInfoRepository orderInfoRepository;

    public long countDoneOrders(long userId) {
        return orderInfoRepository.countDoneOrders(userId);
    }

    public long countDoneSeparateOrders(long userId) {
        return orderInfoRepository.countDoneSeparateOrders(userId);
    }

    public long countDoneGreenOrders(long userId) {
        return orderInfoRepository.countDoneGreenOrders(userId);
    }

    public long countDistinctFractionsInDoneSeparateOrders(long userId) {
        return orderInfoRepository.countDistinctFractionsInDoneSeparateOrders(userId);
    }
}
