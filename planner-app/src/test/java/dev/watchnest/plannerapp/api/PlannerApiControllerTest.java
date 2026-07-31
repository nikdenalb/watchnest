package dev.watchnest.plannerapp.api;

import dev.watchnest.plannerapp.integration.IntegrationEventPublisher;
import dev.watchnest.plannerapp.integration.PlannerIntegrationEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlannerApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IntegrationEventPublisher integrationEventPublisher;

    @Test
    void dashboardReturnsPersonalLibraryAndQuota() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("You"))
                .andExpect(jsonPath("$.status.episodeLimit").exists())
                .andExpect(jsonPath("$.status.episodesWatched").value(0))
                .andExpect(jsonPath("$.todayEvents").isEmpty());
    }

    @Test
    void logsWatchEventAndPublishesIntegrationEvent() throws Exception {
        mockMvc.perform(post("/api/v1/watch-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"Blue Tractor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentTitle").value("Blue Tractor"))
                .andExpect(jsonPath("$.ownerId").exists());

        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.WatchEventRecorded.class));

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.episodesWatched").value(1))
                .andExpect(jsonPath("$.todayEvents[0].contentTitle").value("Blue Tractor"));
    }

    @Test
    void rejectsBlankWatchTitle() throws Exception {
        mockMvc.perform(post("/api/v1/watch-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentTitle":"   "}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesPolicyAndPublishesIntegrationEvent() throws Exception {
        mockMvc.perform(put("/api/v1/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":3,"weekendEpisodeLimit":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekdayEpisodeLimit").value(3))
                .andExpect(jsonPath("$.weekendEpisodeLimit").value(5));

        verify(integrationEventPublisher).publish(any(PlannerIntegrationEvent.ScreenTimePolicyUpdated.class));

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policy.weekdayEpisodeLimit").value(3))
                .andExpect(jsonPath("$.policy.weekendEpisodeLimit").value(5));
    }

    @Test
    void rejectsPolicyOutsideAllowedRange() throws Exception {
        mockMvc.perform(put("/api/v1/policy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekdayEpisodeLimit":21,"weekendEpisodeLimit":5}
                                """))
                .andExpect(status().isBadRequest());
    }
}
