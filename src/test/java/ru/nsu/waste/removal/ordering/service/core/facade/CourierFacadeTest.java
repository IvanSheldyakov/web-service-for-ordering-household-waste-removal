package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.waste.removal.ordering.service.app.view.CourierPanelViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierPanelService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierFacadeTest {

    @Mock
    private CourierPanelService courierPanelService;

    @Mock
    private CourierInfoService courierInfoService;

    @InjectMocks
    private CourierFacade courierFacade;

    @Test
    void getPanel_convertsOrderTimesToCourierTimezone_andLocalizesStatus() {
        long courierId = 10L;
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-03T06:30:00+00:00");
        OffsetDateTime pickupFrom = OffsetDateTime.parse("2026-03-03T07:00:00+00:00");
        OffsetDateTime pickupTo = OffsetDateTime.parse("2026-03-03T09:00:00+00:00");

        when(courierPanelService.getPanel(courierId)).thenReturn(
                new CourierPanel(
                        courierId,
                        "Иванов Иван Иванович",
                        "630000",
                        40L,
                        List.of(
                                new CourierOrderInfo(
                                        77L,
                                        createdAt,
                                        100L,
                                        "630000",
                                        "Новосибирск",
                                        "Ленина, 1",
                                        "SEPARATE",
                                        "NEW",
                                        pickupFrom,
                                        pickupTo,
                                        true,
                                        List.of("Бумага")
                                )
                        ),
                        List.of()
                )
        );
        when(courierInfoService.getProfile(courierId)).thenReturn(
                new CourierProfileInfo(courierId, "Иванов Иван Иванович", "630000", "Asia/Novosibirsk", 40L)
        );

        CourierPanelViewModel viewModel = courierFacade.getPanel(courierId);

        assertEquals(1, viewModel.availableOrders().size());
        CourierPanelViewModel.CourierOrderViewModel order = viewModel.availableOrders().getFirst();
        assertEquals("Новый", order.status());
        assertEquals(OffsetDateTime.parse("2026-03-03T14:00:00+07:00"), order.pickupFrom());
        assertEquals(OffsetDateTime.parse("2026-03-03T16:00:00+07:00"), order.pickupTo());
    }
}
