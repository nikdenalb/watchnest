package dev.watchnest.plannerapp.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class MutableClock extends Clock {

    private Instant instant;
    private final ZoneId zone;

    public MutableClock(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant");
        this.zone = ZoneOffset.UTC;
    }

    public void setInstant(Instant instant) {
        this.instant = Objects.requireNonNull(instant, "instant");
    }

    public void advance(Duration duration) {
        instant = instant.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
