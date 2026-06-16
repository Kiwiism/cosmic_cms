package server.agent;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AgentPlannerService {
    private final AgentGoalRepository goalRepository;
    private final AgentScriptRunner scriptRunner;
    private final AgentScriptRepository scriptRepository;

    public AgentPlannerService(
            AgentGoalRepository goalRepository,
            AgentScriptRunner scriptRunner,
            AgentScriptRepository scriptRepository
    ) {
        this.goalRepository = goalRepository;
        this.scriptRunner = scriptRunner;
        this.scriptRepository = scriptRepository;
    }

    public AgentPlan plan(AgentManagedCharacter managed, AgentPerceptionSnapshot perception, AgentKnowledgeSnapshot knowledge) throws SQLException {
        Optional<AgentGoal> goal = goalRepository.findNextActiveGoal(managed.profileId());
        if (goal.isPresent()) {
            return planGoal(goal.get(), perception, knowledge);
        }
        return planScriptFallback(managed.profile());
    }

    private AgentPlan planGoal(AgentGoal goal, AgentPerceptionSnapshot perception, AgentKnowledgeSnapshot knowledge) {
        String goalType = goal.goalType() == null ? "" : goal.goalType().trim().toUpperCase(Locale.ROOT);
        AgentIntent intent = switch (goalType) {
            case "IDLE" -> AgentIntent.idle(1000);
            case "WAIT" -> AgentIntent.waitFor(1000);
            case "CHAT", "SAY" -> AgentIntent.say(valueOr(goal.targetRef(), "Hello."));
            case "ROAM" -> AgentIntent.roam(valueOr(goal.targetRef(), "goal:" + goal.id()));
            case "MOVE", "MOVE_TO_MAP", "TRAVEL" -> goal.targetMap() == null
                    ? AgentIntent.roam(valueOr(goal.targetRef(), "find route"))
                    : AgentIntent.moveToMap(String.valueOf(goal.targetMap()));
            case "NPC", "TALK_TO_NPC" -> AgentIntent.npc(valueOr(goal.targetRef(), "nearest"));
            case "SHOP" -> AgentIntent.shop(valueOr(goal.targetRef(), "nearest"));
            case "GRIND", "GRIND_TO_LEVEL" -> AgentIntent.grind(valueOr(goal.targetRef(), bestVisibleMonster(perception)));
            case "LOOT" -> AgentIntent.loot(valueOr(goal.targetRef(), bestVisibleDrop(perception)));
            case "USE_ITEM" -> AgentIntent.useItem(valueOr(goal.targetRef(), "potion"));
            case "EQUIP" -> AgentIntent.equip(valueOr(goal.targetRef(), "best available"));
            default -> AgentIntent.waitFor(1000);
        };
        String reason = "Selected goal #" + goal.id() + " (" + goal.goalType() + ") at priority " + goal.priority()
                + " for level " + knowledge.level() + " job " + knowledge.jobId();
        return new AgentPlan(intent, goal, "agent_goals:" + goal.id(), reason);
    }

    private AgentPlan planScriptFallback(AgentProfile profile) throws SQLException {
        ScriptBody scriptBody = resolveScriptBody(profile);
        List<AgentIntent> intents = scriptRunner.parse(scriptBody.body());
        AgentIntent intent = intents.get(0);
        return new AgentPlan(intent, null, scriptBody.source(), "No active goal; using " + scriptBody.source());
    }

    private String bestVisibleMonster(AgentPerceptionSnapshot perception) {
        return perception.nearbyMonsters().stream()
                .findFirst()
                .map(monster -> monster.templateId() == null ? monster.name() : String.valueOf(monster.templateId()))
                .orElse("nearest monster");
    }

    private String bestVisibleDrop(AgentPerceptionSnapshot perception) {
        return perception.nearbyDrops().stream()
                .findFirst()
                .map(drop -> drop.templateId() == null ? "meso" : String.valueOf(drop.templateId()))
                .orElse("nearest drop");
    }

    private ScriptBody resolveScriptBody(AgentProfile profile) throws SQLException {
        String scriptName = profile.scriptName();
        if (scriptName != null && scriptName.strip().startsWith(AgentPilotService.INLINE_SCRIPT_PREFIX)) {
            return new ScriptBody(
                    "inline script_name",
                    scriptName.strip().substring(AgentPilotService.INLINE_SCRIPT_PREFIX.length()).strip()
            );
        }

        Optional<AgentScript> script = scriptRepository.findEnabledByName(scriptName);
        if (script.isPresent()) {
            AgentScript agentScript = script.get();
            return new ScriptBody("agent_scripts:" + agentScript.name() + "@v" + agentScript.version(), agentScript.body());
        }

        return new ScriptBody("default idle fallback", "");
    }

    private String valueOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ScriptBody(String source, String body) {
    }
}
