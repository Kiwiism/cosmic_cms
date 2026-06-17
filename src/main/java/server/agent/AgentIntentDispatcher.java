package server.agent;

import server.agent.actions.AgentActionContext;
import server.agent.actions.AgentActionResult;
import server.agent.actions.AgentActionService;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Policy-gated execution boundary for planned agent intents.
 *
 * The first dispatcher only executes harmless timing intents. Gameplay-facing
 * intents are logged as blocked until their dedicated systems exist.
 */
public final class AgentIntentDispatcher {
    private final AgentRuntimeService runtimeService;
    private final AgentIntentPolicyService policyService;
    private final AgentIntentCooldownService cooldownService;
    private final AgentActionService actionService;

    public AgentIntentDispatcher(
            AgentRuntimeService runtimeService,
            AgentIntentPolicyService policyService,
            AgentIntentCooldownService cooldownService,
            AgentActionService actionService
    ) {
        this.runtimeService = runtimeService;
        this.policyService = policyService;
        this.cooldownService = cooldownService;
        this.actionService = actionService;
    }

    public AgentIntentDispatchResult dispatch(
            AgentManagedCharacter managed,
            AgentIntent intent,
            AgentPerceptionSnapshot perception,
            String scriptSource
    ) throws SQLException {
        AgentIntentPolicyDecision decision = policyService.evaluate(managed.profile(), intent);
        AgentActionResult actionResult;
        if (!decision.allowed()) {
            actionResult = AgentActionResult.blockedByPolicy(decision.capability(), decision.message());
        } else {
            AgentIntentCooldownDecision cooldownDecision = cooldownService.evaluate(managed.profile(), managed.session(), intent);
            if (!cooldownDecision.allowed()) {
                actionResult = AgentActionResult.blockedByRuntime(
                        decision.capability(),
                        cooldownDecision.message(),
                        cooldownDetailsJson(intent, cooldownDecision)
                );
            } else {
                actionResult = actionService.execute(new AgentActionContext(
                        managed,
                        intent,
                        perception,
                        decision,
                        scriptSource,
                        Instant.now()
                ));
            }
        }

        AgentIntentDispatchResult result = AgentIntentDispatchResult.fromActionResult(intent, actionResult);
        if (result.gameplayMutated()) {
            runtimeService.updateSessionLocation(
                    managed.session(),
                    managed.client().getWorld(),
                    managed.client().getChannel(),
                    managed.character().getMapId(),
                    "Gameplay action moved agent through " + intent.type()
            );
        }
        runtimeService.logDispatchedIntent(managed, intent, perception, scriptSource, result);
        return result;
    }

    private String cooldownDetailsJson(AgentIntent intent, AgentIntentCooldownDecision decision) {
        return "{"
                + "\"cooldownState\":\"BLOCKED\","
                + "\"intent\":\"" + intent.type().name() + "\","
                + "\"cooldownMillis\":" + decision.cooldownMillis() + ","
                + "\"remainingMillis\":" + decision.remainingMillis() + ","
                + "\"message\":\"" + decision.message().replace("\\", "\\\\").replace("\"", "\\\"") + "\""
                + "}";
    }
}
