package ru.nsu.waste.removal.ordering.service.core.service.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.order.OrderFiltersInPeriod;
import ru.nsu.waste.removal.ordering.service.core.repository.order.OrderInfoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderInfoService {

    private static final int DEFAULT_ACTIVE_ORDERS_LIMIT = 10;

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

    public List<ActiveOrderInfo> findActiveOrders(long userId) {
        return orderInfoRepository.findActiveOrdersByUserId(userId, DEFAULT_ACTIVE_ORDERS_LIMIT);
    }

    public long countDistinctFractionsInDoneSeparateOrders(long userId) {
        return orderInfoRepository.countDistinctFractionsInDoneSeparateOrders(userId);
    }

    public long countOrdersByFiltersInPeriod(OrderFiltersInPeriod filters) {
        return orderInfoRepository.countOrdersByFiltersInPeriod(filters);
    }

    public long countDistinctFractionsByFiltersInPeriod(OrderFiltersInPeriod filters) {
        return orderInfoRepository.countDistinctFractionsByFiltersInPeriod(filters);
    }

    public List<String> findDistinctFractionNamesByFiltersInPeriod(OrderFiltersInPeriod filters) {
        return orderInfoRepository.findDistinctFractionNamesByFiltersInPeriod(filters);
    }
}
