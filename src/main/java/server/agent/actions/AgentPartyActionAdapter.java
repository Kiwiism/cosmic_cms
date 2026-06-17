package server.agent.actions;

import client.Character;
import net.server.world.Party;
import net.server.world.PartyCharacter;
import server.agent.AgentIntentCapability;
import server.agent.AgentIntentType;
import server.agent.AgentPerceptionSnapshot;

import java.util.Comparator;
import java.util.Optional;

public final class AgentPartyActionAdapter implements AgentActionAdapter {
    @Override
    public AgentIntentCapability capability() {
        return AgentIntentCapability.PARTY;
    }

    @Override
    public AgentActionResult execute(AgentActionContext context) {
        if (context.intent().type() != AgentIntentType.PARTY) {
            return AgentActionResult.blockedByRuntime(capability(), context.intent().type() + " reached the party adapter unexpectedly");
        }

        Character character = context.managed().character();
        if (character == null) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot inspect party state without an attached character");
        }
        if (context.perception() == null || !context.perception().available()) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot inspect party targets without an available perception snapshot");
        }

        Optional<AgentPerceptionSnapshot.AgentVisibleObject> target = selectTarget(context, character);
        String state = readinessState(character, target);
        return AgentActionResult.ok(
                capability(),
                partyMessage(character, target, state),
                false,
                partyDetailsJson(context, character, target.orElse(null), state)
        );
    }

    private Optional<AgentPerceptionSnapshot.AgentVisibleObject> selectTarget(AgentActionContext context, Character character) {
        String target = context.intent().argument();
        return context.perception().nearbyPlayers().stream()
                .filter(player -> player.templateId() == null || player.templateId() != character.getId())
                .filter(player -> matchesTarget(player, target))
                .min(Comparator.comparingLong(AgentPerceptionSnapshot.AgentVisibleObject::distanceSq));
    }

    private boolean matchesTarget(AgentPerceptionSnapshot.AgentVisibleObject player, String target) {
        if (target == null || target.isBlank() || "nearest".equalsIgnoreCase(target.trim())) {
            return true;
        }
        String trimmed = target.trim();
        if (player.name() != null && player.name().equalsIgnoreCase(trimmed)) {
            return true;
        }
        if (player.templateId() != null && String.valueOf(player.templateId()).equals(trimmed)) {
            return true;
        }
        return String.valueOf(player.objectId()).equals(trimmed);
    }

    private String readinessState(Character character, Optional<AgentPerceptionSnapshot.AgentVisibleObject> target) {
        Party party = character.getParty();
        if (party != null && party.getMembers().size() >= 6) {
            return "PARTY_FULL";
        }
        if (target.isEmpty()) {
            return party == null ? "NO_PARTY_TARGET" : "PARTY_STATUS";
        }
        Integer targetCharacterId = target.get().templateId();
        if (targetCharacterId != null && character.isPartyMember(targetCharacterId)) {
            return "ALREADY_PARTIED";
        }
        return party == null ? "PARTY_TARGET_READY" : "INVITE_TARGET_READY";
    }

    private String partyMessage(Character character, Optional<AgentPerceptionSnapshot.AgentVisibleObject> target, String state) {
        return switch (state) {
            case "PARTY_TARGET_READY" -> "Party target " + playerLabel(target.get()) + " is visible; future party creation/join logic can decide next";
            case "INVITE_TARGET_READY" -> "Party invite target " + playerLabel(target.get()) + " is visible";
            case "ALREADY_PARTIED" -> "Target " + playerLabel(target.get()) + " is already in the agent's party";
            case "PARTY_FULL" -> "Agent party " + character.getParty().getId() + " is already full";
            case "PARTY_STATUS" -> "Agent party " + character.getParty().getId() + " status inspected";
            default -> "No nearby party target is visible";
        };
    }

    private String partyDetailsJson(
            AgentActionContext context,
            Character character,
            AgentPerceptionSnapshot.AgentVisibleObject target,
            String state
    ) {
        Party party = character.getParty();
        return "{"
                + "\"partyState\":\"" + escapeJson(state) + "\","
                + "\"intent\":\"" + context.intent().type().name() + "\","
                + "\"argument\":\"" + escapeJson(context.intent().argument()) + "\","
                + "\"world\":" + context.perception().world() + ","
                + "\"channel\":" + context.perception().channel() + ","
                + "\"mapId\":" + context.perception().mapId() + ","
                + "\"agentPosition\":{\"x\":" + context.perception().x() + ",\"y\":" + context.perception().y() + "},"
                + "\"party\":" + partyJson(party, character) + ","
                + "\"target\":" + (target == null ? "null" : playerJson(target)) + ","
                + "\"mutationEnabled\":false,"
                + "\"note\":\"Readiness only; party creation, invites, joins, and leaves are intentionally not executed yet\""
                + "}";
    }

    private String partyJson(Party party, Character character) {
        if (party == null) {
            return "{"
                    + "\"partyId\":null,"
                    + "\"memberCount\":0,"
                    + "\"leader\":false,"
                    + "\"members\":[]"
                    + "}";
        }
        StringBuilder members = new StringBuilder("[");
        int index = 0;
        for (PartyCharacter member : party.getMembers()) {
            if (index++ > 0) {
                members.append(',');
            }
            members.append("{")
                    .append("\"characterId\":").append(member.getId()).append(',')
                    .append("\"name\":\"").append(escapeJson(member.getName())).append("\",")
                    .append("\"level\":").append(member.getLevel()).append(',')
                    .append("\"jobId\":").append(member.getJobId()).append(',')
                    .append("\"world\":").append(member.getWorld()).append(',')
                    .append("\"channel\":").append(member.getChannel()).append(',')
                    .append("\"mapId\":").append(member.getMapId()).append(',')
                    .append("\"online\":").append(member.isOnline())
                    .append("}");
        }
        members.append(']');
        return "{"
                + "\"partyId\":" + party.getId() + ","
                + "\"memberCount\":" + party.getMembers().size() + ","
                + "\"leaderId\":" + party.getLeaderId() + ","
                + "\"leader\":" + (party.getLeaderId() == character.getId()) + ","
                + "\"members\":" + members
                + "}";
    }

    private String playerJson(AgentPerceptionSnapshot.AgentVisibleObject player) {
        return "{"
                + "\"objectId\":" + player.objectId() + ","
                + "\"characterId\":" + nullableNumber(player.templateId()) + ","
                + "\"name\":\"" + escapeJson(player.name()) + "\","
                + "\"position\":{\"x\":" + player.x() + ",\"y\":" + player.y() + "},"
                + "\"distanceSq\":" + player.distanceSq() + ","
                + "\"level\":" + nullableNumber(player.level())
                + "}";
    }

    private String playerLabel(AgentPerceptionSnapshot.AgentVisibleObject player) {
        if (player.name() != null && !player.name().isBlank()) {
            return player.name();
        }
        if (player.templateId() != null) {
            return String.valueOf(player.templateId());
        }
        return String.valueOf(player.objectId());
    }

    private String nullableNumber(Number value) {
        return value == null ? "null" : value.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
