package server.agent;

public record AgentGoalProgressDecision(
        String nextStatus,
        String reason,
        boolean terminal
) {
    public static AgentGoalProgressDecision running(String reason) {
        return new AgentGoalProgressDecision("RUNNING", reason, false);
    }

    public static AgentGoalProgressDecision completed(String reason) {
        return new AgentGoalProgressDecision("COMPLETED", reason, true);
    }

    public static AgentGoalProgressDecision failed(String reason) {
        return new AgentGoalProgressDecision("FAILED", reason, true);
    }
}
