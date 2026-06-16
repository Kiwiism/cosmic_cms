package server.agent;

import java.sql.SQLException;

/**
 * Policy-gated execution boundary for planned agent intents.
 *
 * The first dispatcher only executes harmless timing intents. Gameplay-facing
 * intents are logged as blocked until their dedicated systems exist.
 */
public final class AgentIntentDispatcher {
    private final AgentRuntimeService runtimeService;
    private final AgentIntentPolicyService policyService;

    public AgentIntentDispatcher(AgentRuntimeService runtimeService, AgentIntentPolicyService policyService) {
        this.runtimeService = runtimeService;
        this.policyService = policyService;
    }

    public AgentIntentDispatchResult dispatch(
            AgentManagedCharacter managed,
            AgentIntent intent,
            AgentPerceptionSnapshot perception,
            String scriptSource
    ) throws SQLException {
        AgentIntentPolicyDecision decision = policyService.evaluate(managed.profile(), intent);
        AgentIntentDispatchResult result;
        if (!decision.allowed()) {
            result = AgentIntentDispatchResult.blocked(intent, decision.message());
        } else {
            result = switch (intent.type()) {
                case IDLE -> AgentIntentDispatchResult.ok(intent, "Idle intent accepted as a no-op");
                case WAIT -> AgentIntentDispatchResult.ok(intent, "Wait intent accepted as a no-op");
                case UNKNOWN -> AgentIntentDispatchResult.blocked(intent, "Unknown script intent blocked");
                default -> AgentIntentDispatchResult.blockedByRuntime(intent,
                        decision.capability().name() + " capability is policy-enabled but runtime execution is not implemented yet");
            };
        }

        runtimeService.logDispatchedIntent(managed, intent, perception, scriptSource, result);
        return result;
    }
}
