package server.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses lightweight agent scripts into intents without executing them.
 *
 * Supported MVP commands:
 * IDLE [seconds]
 * WAIT [seconds]
 * SAY [message]
 */
public final class AgentScriptRunner {
    private static final long DEFAULT_IDLE_MILLIS = 30_000L;

    public List<AgentIntent> parse(String scriptBody) {
        if (scriptBody == null || scriptBody.isBlank()) {
            return List.of(AgentIntent.idle(DEFAULT_IDLE_MILLIS));
        }

        List<AgentIntent> intents = new ArrayList<>();
        for (String rawLine : scriptBody.split("\\R")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String command = parts[0].toUpperCase(Locale.ROOT);
            String argument = parts.length > 1 ? parts[1].strip() : "";
            intents.add(parseLine(command, argument, line));
        }

        return intents.isEmpty() ? List.of(AgentIntent.idle(DEFAULT_IDLE_MILLIS)) : List.copyOf(intents);
    }

    private AgentIntent parseLine(String command, String argument, String originalLine) {
        return switch (command) {
            case "IDLE" -> AgentIntent.idle(secondsToMillis(argument, DEFAULT_IDLE_MILLIS));
            case "WAIT" -> AgentIntent.waitFor(secondsToMillis(argument, DEFAULT_IDLE_MILLIS));
            case "SAY" -> AgentIntent.say(argument);
            default -> AgentIntent.unknown(originalLine);
        };
    }

    private long secondsToMillis(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Math.max(1L, Long.parseLong(value.trim())) * 1000L;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
