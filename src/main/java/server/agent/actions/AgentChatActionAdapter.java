package server.agent.actions;

import client.Character;
import server.ChatLogger;
import server.agent.AgentIntentCapability;
import tools.PacketCreator;

public final class AgentChatActionAdapter implements AgentActionAdapter {
    private static final int MAX_NORMAL_CHAT_LENGTH = Byte.MAX_VALUE;

    @Override
    public AgentIntentCapability capability() {
        return AgentIntentCapability.CHAT;
    }

    @Override
    public AgentActionResult execute(AgentActionContext context) {
        if (context.managed().character() == null || context.managed().character().getMap() == null) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot chat without an attached character map");
        }

        String message = normalizeMessage(context.intent().argument());
        if (message.isBlank()) {
            return AgentActionResult.blockedByRuntime(capability(),
                    "SAY requires a non-empty message",
                    chatDetailsJson(context, message, false, "EMPTY_MESSAGE", "Message was blank"));
        }
        if (message.length() > MAX_NORMAL_CHAT_LENGTH) {
            return AgentActionResult.blockedByRuntime(capability(),
                    "SAY message exceeds normal chat length",
                    chatDetailsJson(context, message, false, "MESSAGE_TOO_LONG", "Message exceeded " + MAX_NORMAL_CHAT_LENGTH + " characters"));
        }
        if (looksLikeCommand(message)) {
            return AgentActionResult.blockedByRuntime(capability(),
                    "SAY cannot execute command-like text",
                    chatDetailsJson(context, message, false, "COMMAND_LIKE_TEXT", "Command prefixes are blocked for agent chat"));
        }

        Character character = context.managed().character();
        if (character.getMap().isMuted() && !character.isGM()) {
            return AgentActionResult.blockedByRuntime(capability(),
                    "Map " + character.getMapId() + " is muted",
                    chatDetailsJson(context, message, false, "MAP_MUTED", "Normal chat is disabled on this map"));
        }

        if (!character.isHidden()) {
            character.getMap().broadcastMessage(PacketCreator.getChatText(character.getId(), message, character.getWhiteChat(), 0));
            ChatLogger.log(context.managed().client(), "Agent General", message);
        } else {
            character.getMap().broadcastGMMessage(PacketCreator.getChatText(character.getId(), message, character.getWhiteChat(), 0));
            ChatLogger.log(context.managed().client(), "Agent GM General", message);
        }

        return AgentActionResult.ok(capability(),
                "Said '" + message + "' on map " + character.getMapId(),
                true,
                chatDetailsJson(context, message, true, "SENT", "Broadcast through normal general chat packet"));
    }

    private String normalizeMessage(String value) {
        if (value == null) {
            return "";
        }
        return value.strip();
    }

    private boolean looksLikeCommand(String message) {
        if (message.isBlank()) {
            return false;
        }
        char first = message.charAt(0);
        return first == '@' || first == '!' || first == '/' || first == '~';
    }

    private String chatDetailsJson(
            AgentActionContext context,
            String message,
            boolean sent,
            String state,
            String reason
    ) {
        return "{"
                + "\"chatState\":\"" + state + "\","
                + "\"sent\":" + sent + ","
                + "\"reason\":\"" + escapeJson(reason) + "\","
                + "\"message\":\"" + escapeJson(message) + "\","
                + "\"world\":" + context.managed().client().getWorld() + ","
                + "\"channel\":" + context.managed().client().getChannel() + ","
                + "\"mapId\":" + context.managed().character().getMapId()
                + "}";
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
