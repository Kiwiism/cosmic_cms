package server.runtime;

import java.time.Instant;

public final class ServerLifecycleEvent implements RuntimeEvent {
    public enum Type {
        STARTING,
        ONLINE,
        STOPPING,
        STOPPED
    }

    private final Type type;
    private final Instant occurredAt;

    private ServerLifecycleEvent(Type type) {
        this.type = type;
        this.occurredAt = Instant.now();
    }

    public static ServerLifecycleEvent of(Type type) {
        return new ServerLifecycleEvent(type);
    }

    public Type type() {
        return type;
    }

    @Override
    public Instant occurredAt() {
        return occurredAt;
    }
}
