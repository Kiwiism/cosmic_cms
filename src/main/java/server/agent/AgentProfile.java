package server.agent;

import java.time.Instant;

public record AgentProfile(
        int id,
        int characterId,
        AgentOwnershipType ownershipType,
        Integer ownerAccountId,
        Integer ownerCharacterId,
        boolean enabled,
        String displayName,
        String defaultMode,
        String behaviorProfile,
        String personalityProfile,
        String scriptName,
        boolean llmEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}
