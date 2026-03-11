package ru.nsu.waste.removal.ordering.service.core.service.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderPricingService {

    private final long fixedCostPoints;

    public OrderPricingService(@Value("${app.order.fixed-cost-points:100}") long fixedCostPoints) {
        if (fixedCostPoints <= 0L) {
            throw new IllegalStateException("Fixed order cost in points must be positive");
        }
        this.fixedCostPoints = fixedCostPoints;
    }

    public long getFixedCostPoints() {
        return fixedCostPoints;
    }
}

