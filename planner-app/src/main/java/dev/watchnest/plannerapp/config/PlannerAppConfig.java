package dev.watchnest.plannerapp.config;

import dev.watchnest.planner.policy.ScreenTimeQuotaCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PlannerAppConfig {

    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    ScreenTimeQuotaCalculator screenTimeQuotaCalculator() {
        return new ScreenTimeQuotaCalculator();
    }
}
