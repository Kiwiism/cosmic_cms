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

    public AgentIntentDispatcher(AgentRuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public AgentIntentDispatchResult dispatch(
            AgentManagedCharacter managed,
            AgentIntent intent,
            AgentPerceptionSnapshot perception,
            String scriptSource
    ) throws SQLException {
        AgentIntentDispatchResult result = switch (intent.type()) {
            case IDLE -> AgentIntentDispatchResult.ok(intent, "Idle intent accepted as a no-op");
            case WAIT -> AgentIntentDispatchResult.ok(intent, "Wait intent accepted as a no-op");
            case SAY -> AgentIntentDispatchResult.blocked(intent, "Chat intent is not enabled yet");
            case MOVE -> AgentIntentDispatchResult.blocked(intent, "Movement intent is not enabled yet");
            case UNKNOWN -> AgentIntentDispatchResult.blocked(intent, "Unknown script intent blocked");
        };

        runtimeService.logDispatchedIntent(managed, intent, perception, scriptSource, result);
        return result;
    }
}
