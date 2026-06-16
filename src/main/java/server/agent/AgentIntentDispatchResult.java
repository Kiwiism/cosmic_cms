package server.agent;

import java.time.Instant;

public record AgentIntentDispatchResult(
        AgentIntent intent,
        AgentActionStatus status,
        String message,
        AgentIntentCapability capability,
        boolean policyAllowed,
        Instant dispatchedAt
) {
    public static AgentIntentDispatchResult ok(AgentIntent intent, String message) {
        AgentIntentCapability capability = AgentIntentCapability.fromIntent(intent.type());
        return new AgentIntentDispatchResult(intent, AgentActionStatus.OK, message, capability, true, Instant.now());
    }

    public static AgentIntentDispatchResult blocked(AgentIntent intent, String message) {
        AgentIntentCapability capability = AgentIntentCapability.fromIntent(intent.type());
        return new AgentIntentDispatchResult(intent, AgentActionStatus.BLOCKED, message, capability, false, Instant.now());
    }

    public static AgentIntentDispatchResult blockedByRuntime(AgentIntent intent, String message) {
        AgentIntentCapability capability = AgentIntentCapability.fromIntent(intent.type());
        return new AgentIntentDispatchResult(intent, AgentActionStatus.BLOCKED, message, capability, true, Instant.now());
    }
}
