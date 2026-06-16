package server.agent;

import java.time.Instant;

public record AgentGoal(
        long id,
        int agentProfileId,
        String goalType,
        int priority,
        String status,
        Integer targetWorld,
        Integer targetChannel,
        Integer targetMap,
        String targetRef,
        String parametersJson,
        String progressJson,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant completedAt
) {
}
