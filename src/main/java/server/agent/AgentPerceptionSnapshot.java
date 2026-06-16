package server.agent;

import java.time.Instant;

public record AgentPerceptionSnapshot(
        boolean available,
        int world,
        int channel,
        int mapId,
        int players,
        int monsters,
        int drops,
        int reactors,
        String message,
        Instant capturedAt
) {
    public static AgentPerceptionSnapshot unavailable(AgentSpawnPlan plan, String message) {
        return new AgentPerceptionSnapshot(
                false,
                plan.world() == null ? -1 : plan.world(),
                plan.channel() == null ? -1 : plan.channel(),
                plan.mapId() == null ? -1 : plan.mapId(),
                0,
                0,
                0,
                0,
                message,
                Instant.now()
        );
    }
}
