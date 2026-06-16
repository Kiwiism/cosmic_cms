package server.agent;

import java.time.Instant;

public record AgentScript(
        int id,
        String name,
        int version,
        boolean enabled,
        String scriptType,
        String body,
        Instant updatedAt
) {
}
