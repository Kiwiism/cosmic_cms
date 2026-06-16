package server.agent;

public record AgentPlan(
        AgentIntent intent,
        AgentGoal goal,
        String source,
        String reason
) {
    public boolean hasGoal() {
        return goal != null;
    }
}
