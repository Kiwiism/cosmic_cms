package server.agent;

public record AgentIntentPolicyDecision(
        AgentIntentCapability capability,
        boolean allowed,
        String message
) {
    public static AgentIntentPolicyDecision allowed(AgentIntentCapability capability) {
        return new AgentIntentPolicyDecision(capability, true, capability.name() + " capability is enabled");
    }

    public static AgentIntentPolicyDecision blocked(AgentIntentCapability capability) {
        return new AgentIntentPolicyDecision(capability, false, capability.name() + " capability is disabled by policy");
    }
}
