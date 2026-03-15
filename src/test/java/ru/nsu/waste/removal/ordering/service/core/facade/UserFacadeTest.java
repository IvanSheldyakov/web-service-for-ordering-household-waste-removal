package ru.nsu.waste.removal.ordering.service.core.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.nsu.waste.removal.ordering.service.app.view.UserHomeViewModel;
import ru.nsu.waste.removal.ordering.service.core.model.order.ActiveOrderInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserGreenSlotContext;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserProfileInfo;
import ru.nsu.waste.removal.ordering.service.core.model.user.UserType;
import ru.nsu.waste.removal.ordering.service.core.repository.level.LevelRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.AchieverProfileRepository;
import ru.nsu.waste.removal.ordering.service.core.repository.user.UserLeaderboardRepository;
import ru.nsu.waste.removal.ordering.service.core.service.achievement.AchievementService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.EcoDashboardService;
import ru.nsu.waste.removal.ordering.service.core.service.ecoprofile.UserHistoryService;
import ru.nsu.waste.removal.ordering.service.core.service.ecotask.EcoTaskService;
import ru.nsu.waste.removal.ordering.service.core.service.infocard.InfoCardService;
import ru.nsu.waste.removal.ordering.service.core.service.order.OrderInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserInfoService;
import ru.nsu.waste.removal.ordering.service.core.service.user.UserLeaderboardService;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFacadeTest {

    @Mock
    private UserInfoService userInfoService;

    @Mock
    private OrderInfoService orderInfoService;

    @Mock
    private AchieverProfileRepository achieverProfileRepository;

    @Mock
    private LevelRepository levelRepository;

    @Mock
    private UserLeaderboardRepository userLeaderboardRepository;

    @Mock
    private UserLeaderboardService userLeaderboardService;

    @Mock
    private AchievementService achievementService;

    @Mock
    private EcoTaskService ecoTaskService;

    @Mock
    private InfoCardService infoCardService;

    @Mock
    private EcoDashboardService ecoDashboardService;

    @Mock
    private UserHistoryService userHistoryService;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserFacade userFacade;

    @Test
    void getHome_convertsActiveOrderTimeToUserTimezoneAndMapsPaymentInfo() {
        long userId = 42L;
        when(userInfoService.getProfileByUserId(userId)).thenReturn(
                new UserProfileInfo(userId, UserType.EXPLORER, 100L, 40L, "630000")
        );
        when(userInfoService.getGreenSlotContextByUserId(userId)).thenReturn(
                new UserGreenSlotContext(userId, "630000", "Asia/Novosibirsk")
        );
        when(infoCardService.findByUserType(UserType.EXPLORER)).thenReturn(List.of());
        when(orderInfoService.findActiveOrders(userId)).thenReturn(List.of(
                new ActiveOrderInfo(
                        11L,
                        "SEPARATE",
                        "NEW",
                        OffsetDateTime.parse("2026-03-03T07:00:00+00:00"),
                        OffsetDateTime.parse("2026-03-03T09:00:00+00:00"),
                        100L,
                        "PAID_WITH_POINTS",
                        List.of("Пластик")
                )
        ));

        UserHomeViewModel result = userFacade.getHome(userId);

        assertEquals(1, result.activeOrders().size());
        UserHomeViewModel.ActiveOrderViewModel order = result.activeOrders().getFirst();
        assertEquals("Новый", order.status());
        assertEquals(
                OffsetDateTime.parse("2026-03-03T14:00:00+07:00"),
                order.pickupFrom()
        );
        assertEquals(
                OffsetDateTime.parse("2026-03-03T16:00:00+07:00"),
                order.pickupTo()
        );
        assertEquals(100L, order.costPoints());
        assertEquals("Оплачен баллами", order.paymentStatus());
    }
}
