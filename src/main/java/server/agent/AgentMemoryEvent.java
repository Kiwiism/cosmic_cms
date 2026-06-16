package server.agent;

public record AgentMemoryEvent(
        int agentProfileId,
        String eventType,
        int importance,
        Integer relatedCharacterId,
        Integer relatedAgentProfileId,
        Integer mapId,
        String summary,
        String detailsJson
) {
    public AgentMemoryEvent {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Agent memory event type is required");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("Agent memory summary is required");
        }
    }
}
