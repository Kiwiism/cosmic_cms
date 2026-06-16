package server.agent;

import java.time.Instant;

public record AgentIntentDispatchResult(
        AgentIntent intent,
        AgentActionStatus status,
        String message,
        Instant dispatchedAt
) {
    public static AgentIntentDispatchResult ok(AgentIntent intent, String message) {
        return new AgentIntentDispatchResult(intent, AgentActionStatus.OK, message, Instant.now());
    }

    public static AgentIntentDispatchResult blocked(AgentIntent intent, String message) {
        return new AgentIntentDispatchResult(intent, AgentActionStatus.BLOCKED, message, Instant.now());
    }
}
