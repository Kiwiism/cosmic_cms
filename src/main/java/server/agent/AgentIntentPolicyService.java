package server.agent;

import java.sql.SQLException;

public final class AgentIntentPolicyService {
    private final AgentPolicyRepository repository;

    public AgentIntentPolicyService(AgentPolicyRepository repository) {
        this.repository = repository;
    }

    public AgentIntentPolicyDecision evaluate(AgentProfile profile, AgentIntent intent) throws SQLException {
        AgentIntentCapability capability = AgentIntentCapability.fromIntent(intent.type());
        if (repository.isCapabilityEnabled(profile.id(), capability)) {
            return AgentIntentPolicyDecision.allowed(capability);
        }
        return AgentIntentPolicyDecision.blocked(capability);
    }
}
