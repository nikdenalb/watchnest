package dev.watchnest.planner.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LibraryProfileTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void newProfileDefaultsTreatPlanAsWatchedToFalse() {
        LibraryProfile profile = LibraryProfile.newProfile(ID, "You", new ScreenTimePolicy(2, 4));

        assertFalse(profile.treatPlanAsWatched());
        assertEquals(2, profile.screenTimePolicy().weekdayEpisodeLimit());
        assertEquals(4, profile.screenTimePolicy().weekendEpisodeLimit());
    }

    @Test
    void reconstructionPreservesTreatPlanAsWatchedIndependentlyOfPolicy() {
        ScreenTimePolicy policy = new ScreenTimePolicy(1, 3);

        LibraryProfile on = new LibraryProfile(ID, "You", policy, true);
        LibraryProfile off = new LibraryProfile(ID, "You", policy, false);

        assertTrue(on.treatPlanAsWatched());
        assertFalse(off.treatPlanAsWatched());
        assertEquals(policy, on.screenTimePolicy());
        assertEquals(policy, off.screenTimePolicy());
    }

    @Test
    void rejectsBlankDisplayName() {
        ScreenTimePolicy policy = new ScreenTimePolicy(2, 4);

        assertThrows(IllegalArgumentException.class,
                () -> new LibraryProfile(ID, "  ", policy, false));
        assertThrows(IllegalArgumentException.class,
                () -> LibraryProfile.newProfile(ID, "", policy));
    }
}
