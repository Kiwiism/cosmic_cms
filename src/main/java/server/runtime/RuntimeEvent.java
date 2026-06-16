package server.runtime;

import java.time.Instant;

public interface RuntimeEvent {
    Instant occurredAt();
}
