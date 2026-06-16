package server.agent;

public record AgentControlDecision(
        boolean allowed,
        AgentControlDenyReason denyReason,
        String message,
        Integer world,
        Integer channel
) {
    public static AgentControlDecision allowed(int world, int channel) {
        return new AgentControlDecision(true, AgentControlDenyReason.NONE, "Agent control is allowed", world, channel);
    }

    public static AgentControlDecision denied(AgentControlDenyReason reason, String message) {
        return new AgentControlDecision(false, reason, message, null, null);
    }
}
