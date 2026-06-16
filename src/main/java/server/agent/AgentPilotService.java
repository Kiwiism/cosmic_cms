package server.agent;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Dry-run pilot boundary for future agent control.
 *
 * This service reads perception and script data, chooses the next intent, and
 * logs that decision. It deliberately does not move, chat, attack, trade, loot,
 * or call gameplay handlers.
 */
public final class AgentPilotService {
    private static final String INLINE_SCRIPT_PREFIX = "inline:";

    private final AgentPerceptionService perceptionService;
    private final AgentScriptRunner scriptRunner;
    private final AgentScriptRepository scriptRepository;
    private final AgentRuntimeService runtimeService;
    private final AgentIntentDispatcher intentDispatcher;

    public AgentPilotService(
            AgentPerceptionService perceptionService,
            AgentScriptRunner scriptRunner,
            AgentScriptRepository scriptRepository,
            AgentRuntimeService runtimeService,
            AgentIntentDispatcher intentDispatcher
    ) {
        this.perceptionService = perceptionService;
        this.scriptRunner = scriptRunner;
        this.scriptRepository = scriptRepository;
        this.runtimeService = runtimeService;
        this.intentDispatcher = intentDispatcher;
    }

    public AgentPilotTickResult dryRunTick(AgentManagedCharacter managed) throws SQLException {
        if (!managed.enteredWorld()) {
            throw new IllegalStateException("Agent must enter the world before pilot ticks can run");
        }

        AgentPerceptionSnapshot perception = perceptionService.snapshot(managed);
        ScriptBody scriptBody = resolveScriptBody(managed.profile());
        List<AgentIntent> intents = scriptRunner.parse(scriptBody.body());
        AgentIntent intent = intents.get(0);

        String message = "Planned " + intent.type() + " intent from " + scriptBody.source();
        runtimeService.logPlannedIntent(managed, intent, perception, scriptBody.source(), message);
        AgentIntentDispatchResult dispatchResult = intentDispatcher.dispatch(managed, intent, perception, scriptBody.source());
        runtimeService.heartbeat(managed.session(), message);

        return new AgentPilotTickResult(
                managed.profileId(),
                managed.session().id(),
                intent,
                dispatchResult,
                perception,
                scriptBody.source(),
                message,
                Instant.now()
        );
    }

    private ScriptBody resolveScriptBody(AgentProfile profile) throws SQLException {
        String scriptName = profile.scriptName();
        if (scriptName != null && scriptName.strip().startsWith(INLINE_SCRIPT_PREFIX)) {
            return new ScriptBody(
                    "inline script_name",
                    scriptName.strip().substring(INLINE_SCRIPT_PREFIX.length()).strip()
            );
        }

        Optional<AgentScript> script = scriptRepository.findEnabledByName(scriptName);
        if (script.isPresent()) {
            AgentScript agentScript = script.get();
            return new ScriptBody("agent_scripts:" + agentScript.name() + "@v" + agentScript.version(), agentScript.body());
        }

        return new ScriptBody("default idle fallback", "");
    }

    private record ScriptBody(String source, String body) {
    }
}
