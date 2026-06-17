package server.agent;

public record AgentIntentCooldownDecision(
        boolean allowed,
        long cooldownMillis,
        long remainingMillis,
        String message
) {
    public static AgentIntentCooldownDecision allowed(long cooldownMillis) {
        return new AgentIntentCooldownDecision(true, cooldownMillis, 0L, "Intent cooldown allows execution");
    }

    public static AgentIntentCooldownDecision blocked(long cooldownMillis, long remainingMillis, AgentIntentType intentType) {
        return new AgentIntentCooldownDecision(
                false,
                cooldownMillis,
                Math.max(1L, remainingMillis),
                intentType + " is cooling down for another " + Math.max(1L, remainingMillis) + " ms"
        );
    }
}
