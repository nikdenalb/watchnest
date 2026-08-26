package dev.watchnest.catalog.support;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class SequentialUuidSupplier implements Supplier<UUID> {

    private final AtomicInteger next = new AtomicInteger(1);

    @Override
    public UUID get() {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", next.getAndIncrement()));
    }
}
