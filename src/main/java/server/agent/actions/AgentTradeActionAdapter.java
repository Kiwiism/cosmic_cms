package server.agent.actions;

import client.Character;
import server.Trade;
import server.agent.AgentIntentCapability;
import server.agent.AgentIntentType;
import server.agent.AgentPerceptionSnapshot;

import java.util.Comparator;
import java.util.Optional;

public final class AgentTradeActionAdapter implements AgentActionAdapter {
    @Override
    public AgentIntentCapability capability() {
        return AgentIntentCapability.TRADE;
    }

    @Override
    public AgentActionResult execute(AgentActionContext context) {
        if (context.intent().type() != AgentIntentType.TRADE) {
            return AgentActionResult.blockedByRuntime(capability(), context.intent().type() + " reached the trade adapter unexpectedly");
        }

        Character character = context.managed().character();
        if (character == null) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot inspect trade state without an attached character");
        }
        if (context.perception() == null || !context.perception().available()) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot inspect trade targets without an available perception snapshot");
        }

        Optional<AgentPerceptionSnapshot.AgentVisibleObject> target = selectTarget(context, character);
        String state = readinessState(character, target);
        return AgentActionResult.ok(
                capability(),
                tradeMessage(character, target, state),
                false,
                tradeDetailsJson(context, character, target.orElse(null), state)
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
        Trade trade = character.getTrade();
        if (trade != null && trade.isFullTrade()) {
            return "TRADE_OPEN";
        }
        if (target.isEmpty()) {
            return trade == null ? "NO_TRADE_TARGET" : "TRADE_STATUS";
        }
        if (trade == null) {
            return "TRADE_TARGET_READY";
        }

        Integer targetCharacterId = target.get().templateId();
        Trade partner = trade.getPartner();
        Character partnerCharacter = partner == null ? null : partner.getChr();
        if (targetCharacterId != null && partnerCharacter != null && partnerCharacter.getId() == targetCharacterId) {
            return "TRADE_PARTNER_READY";
        }
        return partner == null ? "TRADE_INVITE_PENDING" : "TRADE_BUSY";
    }

    private String tradeMessage(Character character, Optional<AgentPerceptionSnapshot.AgentVisibleObject> target, String state) {
        Trade trade = character.getTrade();
        return switch (state) {
            case "TRADE_TARGET_READY" -> "Trade target " + playerLabel(target.get()) + " is visible; future trade logic can decide next";
            case "TRADE_PARTNER_READY" -> "Trade partner " + playerLabel(target.get()) + " is visible and linked to the current trade";
            case "TRADE_INVITE_PENDING" -> "Agent has a trade object without a linked visible partner yet";
            case "TRADE_BUSY" -> "Agent is already linked to another trade partner";
            case "TRADE_OPEN" -> "Agent trade window is already open";
            case "TRADE_STATUS" -> "Agent trade state inspected";
            default -> trade == null ? "No nearby trade target is visible" : "Agent trade state inspected";
        };
    }

    private String tradeDetailsJson(
            AgentActionContext context,
            Character character,
            AgentPerceptionSnapshot.AgentVisibleObject target,
            String state
    ) {
        return "{"
                + "\"tradeState\":\"" + escapeJson(state) + "\","
                + "\"intent\":\"" + context.intent().type().name() + "\","
                + "\"argument\":\"" + escapeJson(context.intent().argument()) + "\","
                + "\"world\":" + context.perception().world() + ","
                + "\"channel\":" + context.perception().channel() + ","
                + "\"mapId\":" + context.perception().mapId() + ","
                + "\"agentPosition\":{\"x\":" + context.perception().x() + ",\"y\":" + context.perception().y() + "},"
                + "\"trade\":" + tradeJson(character) + ","
                + "\"target\":" + (target == null ? "null" : playerJson(target)) + ","
                + "\"mutationEnabled\":false,"
                + "\"note\":\"Readiness only; trade invites, windows, items, and mesos are intentionally not executed yet\""
                + "}";
    }

    private String tradeJson(Character character) {
        Trade trade = character.getTrade();
        if (trade == null) {
            return "{"
                    + "\"active\":false,"
                    + "\"fullTrade\":false,"
                    + "\"partner\":null,"
                    + "\"offeredItemCount\":0,"
                    + "\"exchangeMeso\":0,"
                    + "\"exchangeFee\":0,"
                    + "\"mesosTradedToday\":" + character.getMesosTraded() + ","
                    + "\"meso\":" + character.getMeso()
                    + "}";
        }

        int exchangeMeso = trade.getExchangeMesos();
        Trade partner = trade.getPartner();
        Character partnerCharacter = partner == null ? null : partner.getChr();
        return "{"
                + "\"active\":true,"
                + "\"fullTrade\":" + trade.isFullTrade() + ","
                + "\"partner\":" + partnerJson(partnerCharacter) + ","
                + "\"offeredItemCount\":" + trade.getItems().size() + ","
                + "\"exchangeMeso\":" + exchangeMeso + ","
                + "\"exchangeFee\":" + Trade.getFee(exchangeMeso) + ","
                + "\"mesosTradedToday\":" + character.getMesosTraded() + ","
                + "\"meso\":" + character.getMeso()
                + "}";
    }

    private String partnerJson(Character partner) {
        if (partner == null) {
            return "null";
        }
        return "{"
                + "\"characterId\":" + partner.getId() + ","
                + "\"name\":\"" + escapeJson(partner.getName()) + "\","
                + "\"level\":" + partner.getLevel() + ","
                + "\"mapId\":" + (partner.getMap() == null ? "null" : partner.getMapId())
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
