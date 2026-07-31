package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class  ScreenTimePolicyTest {

    @Test
    void usesWeekdayLimitFromMondayToFriday() {
        ScreenTimePolicy policy = new ScreenTimePolicy(2, 4);

        assertEquals(2, policy.episodeLimitFor(LocalDate.of(2026, 7, 6)));
        assertEquals(2, policy.episodeLimitFor(LocalDate.of(2026, 7, 10)));
    }

    @Test
    void usesWeekendLimitOnSaturdayAndSunday() {
        ScreenTimePolicy policy = new ScreenTimePolicy(2, 4);

        assertEquals(4, policy.episodeLimitFor(LocalDate.of(2026, 7, 4)));
        assertEquals(4, policy.episodeLimitFor(LocalDate.of(2026, 7, 5)));
    }

    @Test
    void rejectsNegativeLimits() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenTimePolicy(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> new ScreenTimePolicy(2, -1));
    }
}
