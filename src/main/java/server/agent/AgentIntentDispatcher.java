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
            case ROAM -> AgentIntentDispatchResult.blocked(intent, "Roam intent is not enabled yet");
            case SAY -> AgentIntentDispatchResult.blocked(intent, "Chat intent is not enabled yet");
            case MOVE -> AgentIntentDispatchResult.blocked(intent, "Movement intent is not enabled yet");
            case MOVE_TO_MAP -> AgentIntentDispatchResult.blocked(intent, "Map traversal intent is not enabled yet");
            case USE_PORTAL -> AgentIntentDispatchResult.blocked(intent, "Portal intent is not enabled yet");
            case ATTACK -> AgentIntentDispatchResult.blocked(intent, "Attack intent is not enabled yet");
            case GRIND -> AgentIntentDispatchResult.blocked(intent, "Grinding intent is not enabled yet");
            case LOOT -> AgentIntentDispatchResult.blocked(intent, "Loot intent is not enabled yet");
            case NPC -> AgentIntentDispatchResult.blocked(intent, "NPC interaction intent is not enabled yet");
            case SHOP -> AgentIntentDispatchResult.blocked(intent, "Shop interaction intent is not enabled yet");
            case TRADE -> AgentIntentDispatchResult.blocked(intent, "Trade intent is not enabled yet");
            case PARTY -> AgentIntentDispatchResult.blocked(intent, "Party intent is not enabled yet");
            case USE_ITEM -> AgentIntentDispatchResult.blocked(intent, "Use-item intent is not enabled yet");
            case EQUIP -> AgentIntentDispatchResult.blocked(intent, "Equip intent is not enabled yet");
            case UNKNOWN -> AgentIntentDispatchResult.blocked(intent, "Unknown script intent blocked");
        };

        runtimeService.logDispatchedIntent(managed, intent, perception, scriptSource, result);
        return result;
    }
}
