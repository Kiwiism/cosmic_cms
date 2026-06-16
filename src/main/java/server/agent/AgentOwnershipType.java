package server.agent;

public enum AgentOwnershipType {
    SERVER,
    PLAYER,
    HYBRID,
    SIMULATION;

    public static AgentOwnershipType fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return SERVER;
        }
        try {
            return AgentOwnershipType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return SERVER;
        }
    }
}
