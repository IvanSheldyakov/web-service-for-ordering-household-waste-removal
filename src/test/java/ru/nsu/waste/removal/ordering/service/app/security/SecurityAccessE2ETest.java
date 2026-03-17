package ru.nsu.waste.removal.ordering.service.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.jobs.user-action-event-processor.enabled=false")
@AutoConfigureMockMvc
class SecurityAccessE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedUserPageRedirectsToUserLogin() throws Exception {
        mockMvc.perform(get("/user/1/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/login"));
    }

    @Test
    void unauthenticatedCourierPageRedirectsToCourierLogin() throws Exception {
        mockMvc.perform(get("/courier/1/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courier/login"));
    }

    @Test
    void courierCannotAccessUserPages() throws Exception {
        mockMvc.perform(get("/user/1/home").with(user("courier").roles("COURIER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCannotAccessCourierPages() throws Exception {
        mockMvc.perform(get("/courier/1/home").with(user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrationPagesArePublic() throws Exception {
        mockMvc.perform(get("/registration"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/courier/registration"))
                .andExpect(status().isOk());
    }

    @Test
    void loginPagesArePublic() throws Exception {
        mockMvc.perform(get("/user/login"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/courier/login"))
                .andExpect(status().isOk());
    }
}
