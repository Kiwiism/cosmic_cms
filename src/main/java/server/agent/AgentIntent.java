package server.agent;

public record AgentIntent(
        AgentIntentType type,
        String argument,
        long durationMillis
) {
    public static AgentIntent idle(long durationMillis) {
        return new AgentIntent(AgentIntentType.IDLE, null, durationMillis);
    }

    public static AgentIntent waitFor(long durationMillis) {
        return new AgentIntent(AgentIntentType.WAIT, null, durationMillis);
    }

    public static AgentIntent say(String message) {
        return new AgentIntent(AgentIntentType.SAY, message, 0);
    }

    public static AgentIntent unknown(String line) {
        return new AgentIntent(AgentIntentType.UNKNOWN, line, 0);
    }
}
