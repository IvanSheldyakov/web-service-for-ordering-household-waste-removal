package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.waste.removal.ordering.service.app.form.CourierOrderGroupActionForm;
import ru.nsu.waste.removal.ordering.service.app.view.CourierPanelViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderGroup;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierPanel;
import ru.nsu.waste.removal.ordering.service.core.model.courier.CourierProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.courier.CourierPanelService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
    void getPanel_convertsOrderTimesToCourierTimezone_localizesStatus_andBuildsGroupActionLabel() {
        long courierId = 10L;
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-03-03T06:30:00+00:00");
        OffsetDateTime pickupFrom = OffsetDateTime.parse("2026-03-03T07:00:00+00:00");
        OffsetDateTime pickupTo = OffsetDateTime.parse("2026-03-03T09:00:00+00:00");

        CourierOrderInfo firstOrder = new CourierOrderInfo(
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
        );
        CourierOrderInfo secondOrder = new CourierOrderInfo(
                78L,
                createdAt.plusMinutes(5),
                101L,
                "630000",
                "Новосибирск",
                "Ленина, 2",
                "MIXED",
                "NEW",
                pickupFrom,
                pickupTo,
                false,
                List.of()
        );

        when(courierPanelService.getPanel(courierId)).thenReturn(
                new CourierPanel(
                        courierId,
                        "Иванов Иван Иванович",
                        "630000",
                        40L,
                        List.of(new CourierOrderGroup(
                                "630000",
                                pickupFrom,
                                pickupTo,
                                2,
                                1,
                                1,
                                List.of(firstOrder, secondOrder)
                        )),
                        List.of()
                )
        );
        when(courierInfoService.getProfile(courierId)).thenReturn(
                new CourierProfileInfo(courierId, "Иванов Иван Иванович", "630000", "Asia/Novosibirsk", 40L)
        );

        CourierPanelViewModel viewModel = courierFacade.getPanel(courierId);

        assertEquals(1, viewModel.availableOrderGroups().size());
        CourierPanelViewModel.CourierOrderGroupViewModel group = viewModel.availableOrderGroups().getFirst();
        assertEquals("Взять все 2 заказов", group.takeActionLabel());
        assertEquals(OffsetDateTime.parse("2026-03-03T14:00:00+07:00"), group.pickupFrom());
        assertEquals(OffsetDateTime.parse("2026-03-03T16:00:00+07:00"), group.pickupTo());
        assertEquals(2, group.ordersCount());

        CourierPanelViewModel.CourierOrderViewModel firstOrderView = group.orders().getFirst();
        assertEquals("Новый", firstOrderView.status());
        assertEquals(OffsetDateTime.parse("2026-03-03T14:00:00+07:00"), firstOrderView.pickupFrom());
        assertEquals(OffsetDateTime.parse("2026-03-03T16:00:00+07:00"), firstOrderView.pickupTo());
    }

    @Test
    void takeOrderGroup_withInvalidForm_throwsValidationErrorBeforeServiceCall() {
        CourierOrderGroupActionForm form = new CourierOrderGroupActionForm();
        form.setExpectedOrderCount(3);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> courierFacade.takeOrderGroup(100L, form)
        );
        assertEquals("Некорректный ключ группы заказов", exception.getMessage());
    }

    @Test
    void takeOrderGroup_withValidForm_callsService() {
        long courierId = 100L;
        CourierOrderGroupActionForm form = new CourierOrderGroupActionForm();
        form.setClusterKey("630000");
        form.setPickupFrom(OffsetDateTime.parse("2026-03-21T10:00:00+07:00"));
        form.setPickupTo(OffsetDateTime.parse("2026-03-21T12:00:00+07:00"));
        form.setExpectedOrderCount(2);

        courierFacade.takeOrderGroup(courierId, form);

        verify(courierPanelService).takeOrderGroup(
                eq(courierId),
                any(),
                eq(2)
        );
    }
}
