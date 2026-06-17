package server.agent.actions;

import server.agent.AgentActionStatus;
import server.agent.AgentIntentCapability;

import java.time.Instant;

public record AgentActionResult(
        AgentActionStatus status,
        AgentIntentCapability capability,
        String message,
        boolean policyAllowed,
        boolean gameplayMutated,
        boolean dryRun,
        String detailsJson,
        Instant completedAt
) {
    public AgentActionResult {
        completedAt = completedAt == null ? Instant.now() : completedAt;
    }

    public static AgentActionResult ok(AgentIntentCapability capability, String message, boolean gameplayMutated) {
        return new AgentActionResult(AgentActionStatus.OK, capability, message, true, gameplayMutated, !gameplayMutated, null, Instant.now());
    }

    public static AgentActionResult ok(AgentIntentCapability capability, String message, boolean gameplayMutated, String detailsJson) {
        return new AgentActionResult(AgentActionStatus.OK, capability, message, true, gameplayMutated, !gameplayMutated, detailsJson, Instant.now());
    }

    public static AgentActionResult blockedByPolicy(AgentIntentCapability capability, String message) {
        return new AgentActionResult(AgentActionStatus.BLOCKED, capability, message, false, false, true, null, Instant.now());
    }

    public static AgentActionResult blockedByRuntime(AgentIntentCapability capability, String message) {
        return new AgentActionResult(AgentActionStatus.BLOCKED, capability, message, true, false, true, null, Instant.now());
    }

    public static AgentActionResult blockedByRuntime(AgentIntentCapability capability, String message, String detailsJson) {
        return new AgentActionResult(AgentActionStatus.BLOCKED, capability, message, true, false, true, detailsJson, Instant.now());
    }

    public static AgentActionResult failed(AgentIntentCapability capability, String message) {
        return new AgentActionResult(AgentActionStatus.FAILED, capability, message, true, false, true, null, Instant.now());
    }
}
